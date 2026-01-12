package de.keksuccino.fancymenu.customization.element.elements.item;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import java.util.function.BiConsumer;

public class ItemEditorElement extends AbstractEditorElement<ItemEditorElement, ItemElement> {

    public ItemEditorElement(@NotNull ItemElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        ConsumingSupplier<ItemEditorElement, String> itemKeyTargetFieldGetter = consumes -> consumes.element.itemKey;
        BiConsumer<ItemEditorElement, String> itemKeyTargetFieldSetter = (itemEditorElement, s) -> itemEditorElement.element.itemKey = s;

        ContextMenu.ClickableContextMenuEntry<?> itemKeyEntry = this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_key", ItemEditorElement.class,
                        itemKeyTargetFieldGetter,
                        itemKeyTargetFieldSetter,
                        null, false, true, Component.translatable("fancymenu.elements.item.key"),
                        true, "" + BuiltInRegistries.ITEM.getKey(Items.BARRIER), null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.key.desc")))
                .setStackable(false);

        if (itemKeyEntry instanceof ContextMenu.SubMenuContextMenuEntry subMenuEntry) {

            subMenuEntry.getSubContextMenu().removeEntry("input_value");

            subMenuEntry.getSubContextMenu().addClickableEntryAt(0, "input_value", Component.translatable("fancymenu.common_components.set"), (menu, entry) ->
            {
                if (entry.getStackMeta().isFirstInStack()) {
                    Screen inputScreen = new ItemKeyScreen(itemKeyTargetFieldGetter.get(this), callback -> {
                        if (callback != null) {
                            this.editor.history.saveSnapshot();
                            itemKeyTargetFieldSetter.accept(this, callback);
                        }
                        menu.closeMenu();
                        this.openContextMenuScreen(this.editor);
                    });
                    this.openContextMenuScreen(inputScreen);
                }
            }).setStackable(false);

        }

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_name", ItemEditorElement.class,
                consumes -> consumes.element.itemName,
                (itemEditorElement, s) -> itemEditorElement.element.itemName = s,
                null, false, true, Component.translatable("fancymenu.elements.item.name"),
                true, null, null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_count", ItemEditorElement.class,
                consumes -> consumes.element.itemCount,
                (itemEditorElement, s) -> itemEditorElement.element.itemCount = s,
                null, false, true, Component.translatable("fancymenu.elements.item.item_count"),
                true, "1", null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_lore", ItemEditorElement.class, consumes -> {
                    if (consumes.element.lore != null) return consumes.element.lore.replace("%n%", "\n");
                    return "";
                }, (itemEditorElement, s) -> {
                    if (s != null) {
                        itemEditorElement.element.lore = s.replace("\n", "%n%");
                        if (itemEditorElement.element.lore.isBlank()) itemEditorElement.element.lore = null;
                    } else {
                        itemEditorElement.element.lore = null;
                    }
                }, null, true, true, Component.translatable("fancymenu.elements.item.lore"), true, null, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.lore.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "enchanted", ItemEditorElement.class,
                consumes -> consumes.element.enchanted,
                (itemEditorElement, aBoolean) -> itemEditorElement.element.enchanted = aBoolean,
                "fancymenu.elements.item.enchanted");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "show_tooltip", ItemEditorElement.class,
                consumes -> consumes.element.showTooltip,
                (itemEditorElement, aBoolean) -> itemEditorElement.element.showTooltip = aBoolean,
                "fancymenu.elements.item.show_tooltip");

        this.rightClickMenu.addSeparatorEntry("separator_before_nbt");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "nbt_data", ItemEditorElement.class,
                        consumes -> consumes.element.nbtData,
                        (itemEditorElement, s) -> itemEditorElement.element.nbtData = s,
                        null, false, true, Component.translatable("fancymenu.elements.item.nbt"),
                        true, null, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.nbt.desc")));

    }


}
