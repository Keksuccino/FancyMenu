package de.keksuccino.fancymenu.customization.element.elements.item;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import java.util.function.BiConsumer;

public class ItemEditorElement extends AbstractEditorElement {

    public ItemEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        ConsumingSupplier<ItemEditorElement, String> itemKeyTargetFieldGetter = consumes -> consumes.getElement().itemKey;
        BiConsumer<ItemEditorElement, String> itemKeyTargetFieldSetter = (itemEditorElement, s) -> itemEditorElement.getElement().itemKey = s;

        ContextMenu.ClickableContextMenuEntry<?> itemKeyEntry = this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_key", ItemEditorElement.class,
                        itemKeyTargetFieldGetter,
                        itemKeyTargetFieldSetter,
                        null, false, true, Component.translatable("fancymenu.elements.item.key"),
                        true, "" + BuiltInRegistries.ITEM.getKey(Items.BARRIER), null, null)
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
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(inputScreen);
                }
            }).setStackable(false);

        }

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_name", ItemEditorElement.class,
                consumes -> consumes.getElement().itemName,
                (itemEditorElement, s) -> itemEditorElement.getElement().itemName = s,
                null, false, true, Component.translatable("fancymenu.elements.item.name"),
                true, null, null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_count", ItemEditorElement.class,
                consumes -> consumes.getElement().itemCount,
                (itemEditorElement, s) -> itemEditorElement.getElement().itemCount = s,
                null, false, true, Component.translatable("fancymenu.elements.item.item_count"),
                true, "1", null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_lore", ItemEditorElement.class, consumes -> {
                    if (consumes.getElement().lore != null) return consumes.getElement().lore.replace("%n%", "\n");
                    return "";
                }, (itemEditorElement, s) -> {
                    if (s != null) {
                        itemEditorElement.getElement().lore = s.replace("\n", "%n%");
                        if (itemEditorElement.getElement().lore.isBlank()) itemEditorElement.getElement().lore = null;
                    } else {
                        itemEditorElement.getElement().lore = null;
                    }
                }, null, true, true, Component.translatable("fancymenu.elements.item.lore"), true, null, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.lore.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "enchanted", ItemEditorElement.class,
                consumes -> consumes.getElement().enchanted,
                (itemEditorElement, aBoolean) -> itemEditorElement.getElement().enchanted = aBoolean,
                "fancymenu.elements.item.enchanted");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "show_tooltip", ItemEditorElement.class,
                consumes -> consumes.getElement().showTooltip,
                (itemEditorElement, aBoolean) -> itemEditorElement.getElement().showTooltip = aBoolean,
                "fancymenu.elements.item.show_tooltip");

        this.rightClickMenu.addSeparatorEntry("separator_before_nbt");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "nbt_data", ItemEditorElement.class,
                        consumes -> consumes.getElement().nbtData,
                        (itemEditorElement, s) -> itemEditorElement.getElement().nbtData = s,
                        null, false, true, Component.translatable("fancymenu.elements.item.nbt"),
                        true, null, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.nbt.desc")));

    }

    public ItemElement getElement() {
        return (ItemElement) this.element;
    }

}
