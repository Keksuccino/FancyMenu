package de.keksuccino.fancymenu.customization.element.elements.item;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ItemElementBuilder extends ElementBuilder<ItemElement, ItemEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public ItemElementBuilder() {
        super("item");
    }

    @Override
    public @NotNull ItemElement buildDefaultInstance() {
        ItemElement i = new ItemElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public ItemElement deserializeElement(@NotNull SerializedElement serialized) {

        ItemElement element = this.buildDefaultInstance();

        element.itemKey = Objects.requireNonNullElse(serialized.getValue("item_key"), element.itemKey);
        element.itemCount = Objects.requireNonNullElse(serialized.getValue("item_count"), element.itemCount);
        element.enchanted = SerializationHelper.INSTANCE.deserializeBoolean(element.enchanted, serialized.getValue("enchanted"));
        element.itemName = serialized.getValue("item_name");
        element.lore = serialized.getValue("lore");
        element.showTooltip = SerializationHelper.INSTANCE.deserializeBoolean(element.showTooltip, serialized.getValue("show_tooltip"));
        element.nbtData = serialized.getValue("custom_nbt_data");

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull ItemElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("item_key", element.itemKey);
        serializeTo.putProperty("item_count", element.itemCount);
        serializeTo.putProperty("enchanted", "" + element.enchanted);
        if (element.itemName != null) serializeTo.putProperty("item_name", element.itemName);
        if (element.lore != null) serializeTo.putProperty("lore", element.lore);
        serializeTo.putProperty("show_tooltip", "" + element.showTooltip);
        if (element.nbtData != null) serializeTo.putProperty("custom_nbt_data", element.nbtData);

        return serializeTo;

    }

    @Override
    public @NotNull ItemEditorElement wrapIntoEditorElement(@NotNull ItemElement element, @NotNull LayoutEditorScreen editor) {
        return new ItemEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.item");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.desc");
    }

}
