package de.keksuccino.fancymenu.customization.element.elements.image;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class ImageEditorElement extends AbstractEditorElement<ImageEditorElement, ImageElement> {

    public ImageEditorElement(@NotNull ImageElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.textureSupplier.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.IMAGE);

        this.element.imageTint.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.element.roundingRadius.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.image.rounding_radius.desc")))
                .setIcon(MaterialIcons.ROUNDED_CORNER);

        this.rightClickMenu.addSeparatorEntry("separator_before_repeat_texture");

        this.element.repeat.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsActiveSupplier((menu, entry) -> !this.element.nineSlice.tryGetNonNull())
                .setIcon(MaterialIcons.REPEAT);

        this.rightClickMenu.addSeparatorEntry("separator_before_nine_slice_settings");

        this.element.nineSlice.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsActiveSupplier((menu, entry) -> !this.element.repeat.tryGetNonNull())
                .setIcon(MaterialIcons.GRID_GUIDES);

        this.element.nineSliceBorderX.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsActiveSupplier((menu, entry) -> !this.element.repeat.tryGetNonNull())
                .setIcon(MaterialIcons.BORDER_HORIZONTAL);

        this.element.nineSliceBorderY.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsActiveSupplier((menu, entry) -> !this.element.repeat.tryGetNonNull())
                .setIcon(MaterialIcons.BORDER_VERTICAL);

        this.rightClickMenu.addSeparatorEntry("image_separator_1");

        this.element.restartAnimatedOnMenuLoad.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.REPLAY);

        this.rightClickMenu.addSeparatorEntry("separator_after_restart_animated");

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.image.restore_aspect_ratio"), (menu, entry) -> {
                    List<AbstractEditorElement<?, ?>> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ImageEditorElement));
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement<?, ?> e : selectedElements) {
                        ((ImageElement)e.element).restoreAspectRatio();
                    }
                }).setIcon(MaterialIcons.ASPECT_RATIO);

    }

}
