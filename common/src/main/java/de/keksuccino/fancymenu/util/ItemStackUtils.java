package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

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

}
