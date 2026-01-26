package de.keksuccino.fancymenu.customization.element.elements.shape.rectangle;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class RectangleShapeEditorElement extends AbstractEditorElement<RectangleShapeEditorElement, RectangleShapeElement> {

    public RectangleShapeEditorElement(@NotNull RectangleShapeElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.color.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        ContextMenu cornerRadiusMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("corner_radius", Component.translatable("fancymenu.elements.shape.corner_radius"), cornerRadiusMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.corner_radius.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

        this.element.cornerRadiusTopLeft.buildContextMenuEntryAndAddTo(cornerRadiusMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.corner_radius.top_left.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

        this.element.cornerRadiusTopRight.buildContextMenuEntryAndAddTo(cornerRadiusMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.corner_radius.top_right.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

        this.element.cornerRadiusBottomRight.buildContextMenuEntryAndAddTo(cornerRadiusMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.corner_radius.bottom_right.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

        this.element.cornerRadiusBottomLeft.buildContextMenuEntryAndAddTo(cornerRadiusMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.shape.corner_radius.bottom_left.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

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
