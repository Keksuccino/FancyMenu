package de.keksuccino.fancymenu.customization.element.elements.image;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class ImageEditorElement extends AbstractEditorElement<ImageEditorElement, ImageElement> {

    public ImageEditorElement(@NotNull ImageElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("set_source", Component.translatable("fancymenu.elements.image.set_source"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(ResourceChooserScreen.image(null, source -> {
                if (source != null) {
                    this.editor.history.saveSnapshot();
                    this.getElement().textureSupplier = ResourceSupplier.image(source);
                }
                Minecraft.getInstance().setScreen(this.editor);
            }).setSource((this.getElement().textureSupplier != null) ? this.getElement().textureSupplier.getSourceWithPrefix() : null, false));
        }).setIcon(ContextMenu.IconFactory.getIcon("image"));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "image_tint", ImageEditorElement.class,
                        consumes -> consumes.getElement().imageTint,
                        (imageEditorElement, tint) -> imageEditorElement.getElement().imageTint = Objects.requireNonNullElse(tint, "#FFFFFF"),
                        null, false, true, Component.translatable("fancymenu.elements.image.tint"),
                        true, "#FFFFFF", null, null)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_before_repeat_texture");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "repeat_texture",
                        ImageEditorElement.class,
                        consumes -> consumes.getElement().repeat,
                        (imageEditorElement, aBoolean) -> imageEditorElement.getElement().repeat = aBoolean,
                        "fancymenu.elements.image.repeat")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().nineSlice)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_before_nine_slice_settings");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "nine_slice_texture",
                        ImageEditorElement.class,
                        consumes -> consumes.getElement().nineSlice,
                        (imageEditorElement, aBoolean) -> imageEditorElement.getElement().nineSlice = aBoolean,
                        "fancymenu.elements.image.nine_slice")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat)
                .setStackable(false);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_x",
                        ImageEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderX,
                        (imageEditorElement, integer) -> imageEditorElement.getElement().nineSliceBorderX = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_x"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_y",
                        ImageEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderY,
                        (imageEditorElement, integer) -> imageEditorElement.getElement().nineSliceBorderY = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_y"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.rightClickMenu.addSeparatorEntry("image_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "restart_animated_on_menu_load",
                        ImageEditorElement.class,
                        consumes -> consumes.getElement().restartAnimatedOnMenuLoad,
                        (imageEditorElement, aBoolean) -> imageEditorElement.getElement().restartAnimatedOnMenuLoad = aBoolean,
                        "fancymenu.elements.image.restart_animated_on_menu_load")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_restart_animated");

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.image.restore_aspect_ratio"), (menu, entry) -> {
                    List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ImageEditorElement));
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((ImageElement)e.element).restoreAspectRatio();
                    }
                }).setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

    }

    public ImageElement getElement() {
        return (ImageElement) this.element;
    }

}
