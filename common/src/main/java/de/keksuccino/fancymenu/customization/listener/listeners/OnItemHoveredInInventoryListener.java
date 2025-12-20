package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class OnItemHoveredInInventoryListener extends AbstractListener {

    @Nullable
    private HoveredItemData currentItemData;

    public OnItemHoveredInInventoryListener() {
        super("item_hovered_in_inventory");
    }

    /**
     * @return {@code true} when a new hovered item was detected and listeners were notified.
     */
    public boolean onItemHovered(@Nonnull Slot slot, @Nonnull ItemStack stack) {
        HoveredItemData newData = HoveredItemData.from(slot, stack, this::serializeComponent);
        HoveredItemData existingData = this.currentItemData;

        if ((existingData != null) && existingData.isSameTarget(newData)) {
            this.currentItemData = newData;
            return false;
        }

        this.currentItemData = newData;
        this.notifyAllInstances();
        return true;
    }

    public void clearCurrentItem() {
        this.currentItemData = null;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("item_key", () -> {
            HoveredItemData data = this.currentItemData;
            if (data == null || data.itemKey() == null) {
                return "ERROR";
            }
            return data.itemKey();
        }));
        list.add(new CustomVariable("item_display_name_string", () -> {
            HoveredItemData data = this.currentItemData;
            if (data == null || data.displayNameString() == null) {
                return "ERROR";
            }
            return data.displayNameString();
        }));
        list.add(new CustomVariable("item_display_name_json", () -> {
            HoveredItemData data = this.currentItemData;
            if (data == null || data.displayNameJson() == null) {
                return "ERROR";
            }
            return data.displayNameJson();
        }));
    }

    @Nullable
    private String serializeComponent(@Nonnull Component component) {
        RegistryAccess registryAccess = RegistryAccess.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            registryAccess = minecraft.level.registryAccess();
        } else if ((minecraft.getConnection() != null) && (minecraft.getConnection().registryAccess() != null)) {
            registryAccess = minecraft.getConnection().registryAccess();
        }
        return Component.Serializer.toJson(component, registryAccess);
    }

    @Override
    public @Nonnull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_item_hovered_in_inventory");
    }

    @Override
    public @Nonnull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_item_hovered_in_inventory.desc"));
    }

    public record HoveredItemData(@Nonnull Slot slot,
                                  @Nonnull ItemStack stack,
                                  @Nullable String itemKey,
                                  @Nullable String displayNameString,
                                  @Nullable String displayNameJson) {

        private boolean isSameTarget(@Nonnull HoveredItemData other) {
            return this.slot == other.slot
                && ItemStack.isSameItemSameComponents(this.stack, other.stack);
        }

        public static @Nonnull HoveredItemData from(@Nonnull Slot slot,
                                                    @Nonnull ItemStack stack,
                                                    @Nonnull java.util.function.Function<Component, String> componentSerializer) {
            ItemStack copiedStack = stack.copy();
            ResourceLocation itemKeyLocation = BuiltInRegistries.ITEM.getKey(copiedStack.getItem());
            String itemKey = (itemKeyLocation != null) ? itemKeyLocation.toString() : null;
            Component displayName = copiedStack.getHoverName();
            String displayNameString = displayName.getString();
            String displayNameJson = componentSerializer.apply(displayName);
            return new HoveredItemData(slot, copiedStack, itemKey, displayNameString, displayNameJson);
        }
    }
}
