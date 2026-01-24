package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import org.jetbrains.annotations.NotNull;

public class ShapeEditorElement extends AbstractEditorElement<ShapeEditorElement, ShapeElement> {

    public ShapeEditorElement(@NotNull ShapeElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.color.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.color.desc")))
                .setIcon(MaterialIcons.PALETTE);

    }

}
