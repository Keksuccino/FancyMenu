package de.keksuccino.fancymenu.customization.element.elements.shape.circle;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import org.jetbrains.annotations.NotNull;

public class CircleShapeEditorElement extends AbstractEditorElement<CircleShapeEditorElement, CircleShapeElement> {

    public CircleShapeEditorElement(@NotNull CircleShapeElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.color.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        this.element.roundness.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.circle.roundness.desc")))
                .setIcon(MaterialIcons.CIRCLE);

        this.rightClickMenu.addSeparatorEntry("separator_before_shape_blur");

        this.element.blurEnabled.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.blur.desc")))
                .setIcon(MaterialIcons.BLUR_ON);

        var blurRadiusEntry = this.element.blurRadius.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.blur.radius.desc")))
                .setIcon(MaterialIcons.BLUR_MEDIUM);
        blurRadiusEntry.addIsActiveSupplier((menu, entry) -> this.element.blurEnabled.tryGetNonNull());

        this.rightClickMenu.addSeparatorEntry("separator_after_shape_blur");

    }

}
