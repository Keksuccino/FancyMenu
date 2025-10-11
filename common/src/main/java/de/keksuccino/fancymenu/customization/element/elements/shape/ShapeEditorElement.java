package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ShapeEditorElement extends AbstractEditorElement {

    public ShapeEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_color", ShapeEditorElement.class,
                        consumes -> consumes.getElement().colorRaw,
                        (shapeEditorElement, s) -> shapeEditorElement.getElement().colorRaw = s,
                        null, false, true, Component.translatable("fancymenu.elements.shape.color"),
                        true, "#FFFFFF", null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.color.desc")));

    }

    public ShapeElement getElement() {
        return (ShapeElement) this.element;
    }

}
