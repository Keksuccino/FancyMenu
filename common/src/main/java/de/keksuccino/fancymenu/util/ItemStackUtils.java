package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinHolderSetNamed;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("all")
public final class ItemStackUtils {

    private ItemStackUtils() {
    }

    public static ItemStack createDisplayStack(Item item) {
        return createDisplayStack(item, 1);
    }

    public static ItemStack createDisplayStack(Item item, int count) {
        if ((item == null) || (item == Items.AIR)) {
            return ItemStack.EMPTY;
        }

        Holder<Item> holder = getBoundItemHolder(item);
        if ((holder != null) && holder.areComponentsBound()) {
            return new ItemStack(holder, count);
        }

        DataComponentPatch fallbackComponents = DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable(item.getDescriptionId()))
                .set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item))
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build();
        return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), count, fallbackComponents);
    }

    public static ItemStack createGuiItemStack(Item item) {
        if ((item == null) || (item == Items.AIR)) {
            return ItemStack.EMPTY;
        }

        GuiItemContext.bootstrap();
        return item.getDefaultInstance();
    }

    private static Holder<Item> getBoundItemHolder(Item item) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Optional<Holder.Reference<Item>> levelHolder = minecraft.level.registryAccess().lookupOrThrow(Registries.ITEM).get(key);
            if (levelHolder.isPresent()) {
                return levelHolder.get();
            }
        }

        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            Registry<Item> itemRegistry = connection.registryAccess().lookupOrThrow(Registries.ITEM);
            Optional<Holder.Reference<Item>> connectionHolder = itemRegistry.get(key);
            if (connectionHolder.isPresent()) {
                return connectionHolder.get();
            }
        }

        return null;
    }

    private static final class GuiItemContext {

        private static final HolderLookup.Provider PROVIDER = createProvider();

        private static HolderLookup.Provider createProvider() {
            HolderLookup.Provider baseProvider = VanillaRegistries.createLookup();
            HolderLookup.Provider provider;
            try (MultiPackResourceManager dataResources = new MultiPackResourceManager(
                    PackType.SERVER_DATA,
                    Minecraft.getInstance().getResourcePackRepository().openAllSelected()
            )) {
                provider = HolderLookup.Provider.create(baseProvider.listRegistries().map(lookup -> withLoadedTags(dataResources, lookup)));
            }
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(provider).forEach(pending -> pending.apply());
            return provider;
        }

        private static <T> HolderLookup.RegistryLookup<T> withLoadedTags(MultiPackResourceManager dataResources, HolderLookup.RegistryLookup<T> original) {
            ResourceKey<? extends Registry<T>> registryKey =
                    (ResourceKey<? extends Registry<T>>) original.key();
            Map<TagKey<T>, List<Holder<T>>> loadedTags = TagLoader.loadTagsForRegistry(
                    dataResources,
                    registryKey,
                    TagLoader.ElementLookup.fromGetters(registryKey, original, original)
            );
            if (loadedTags.isEmpty()) {
                return original;
            }

            Map<TagKey<T>, HolderSet.Named<T>> namedTags = new java.util.HashMap<>();
            loadedTags.forEach((tag, holders) -> namedTags.put(tag, createNamedTagSet(original, tag, holders)));
            return new HolderLookup.RegistryLookup.Delegate<>() {
                @Override
                public HolderLookup.RegistryLookup<T> parent() {
                    return original;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                    return Optional.ofNullable(namedTags.get(id)).or(() -> original.get(id));
                }

                @Override
                public Stream<HolderSet.Named<T>> listTags() {
                    return Stream.concat(namedTags.values().stream(), original.listTags().filter(tag -> !namedTags.containsKey(tag.key())));
                }
            };
        }

        private static <T> HolderSet.Named<T> createNamedTagSet(
                HolderLookup.RegistryLookup<T> owner,
                TagKey<T> key,
                List<Holder<T>> contents
        ) {
            HolderSet.Named<T> named = IMixinHolderSetNamed.invoke_new_FancyMenu((HolderOwner<T>) owner, key);
            ((IMixinHolderSetNamed<T>) named).invoke_bind_FancyMenu(contents);
            return named;
        }

        private static void bootstrap() {
            PROVIDER.listRegistryKeys();
        }

        private GuiItemContext() {
        }

    }

}
