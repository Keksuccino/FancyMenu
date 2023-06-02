package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ShapeEditorElement extends AbstractEditorElement {

    public ShapeEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_color", null,
                        consumes -> (consumes instanceof ShapeEditorElement),
                        "#ffffff",
                        consumes -> ((ShapeElement)consumes.element).color.getHex(),
                        (element, colorHex) -> {
                            DrawableColor c = DrawableColor.of(colorHex);
                            if (c != null) {
                                ((ShapeElement)element.element).color = c;
                            }
                        }, false, false, Component.translatable("fancymenu.editor.items.shape.color"))
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.shape.color.btndesc")));

    }

}
