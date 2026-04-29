package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractContainerScreen;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public class HoveredInventoryItemPlaceholder extends AbstractWorldPlaceholder {

    public HoveredInventoryItemPlaceholder() {
        super("hovered_inventory_item");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return "";

        Slot hoveredSlot = ((IMixinAbstractContainerScreen) containerScreen).get_hoveredSlot_FancyMenu();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return "";

        ResourceLocation itemKey = Services.PLATFORM.getItemKey(hoveredSlot.getItem().getItem());
        return itemKey != null ? itemKey.toString() : "";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.hovered_inventory_item";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
