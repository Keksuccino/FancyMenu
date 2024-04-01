package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

//TODO übernehmen
public class MusicControllerEditorElement extends AbstractEditorElement {

    public MusicControllerEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
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

        this.rightClickMenu.addSeparatorEntry("separator_before_repeat_texture");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "repeat_texture",
                        MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().repeat,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.getElement().repeat = aBoolean,
                        "fancymenu.elements.image.repeat")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().nineSlice)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_before_nine_slice_settings");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "nine_slice_texture",
                        MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().nineSlice,
                        (musicControllerEditorElement, aBoolean) -> musicControllerEditorElement.getElement().nineSlice = aBoolean,
                        "fancymenu.elements.image.nine_slice")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat)
                .setStackable(false);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_x",
                        MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderX,
                        (musicControllerEditorElement, integer) -> musicControllerEditorElement.getElement().nineSliceBorderX = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_x"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_y",
                        MusicControllerEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderY,
                        (musicControllerEditorElement, integer) -> musicControllerEditorElement.getElement().nineSliceBorderY = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_y"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.rightClickMenu.addSeparatorEntry("image_separator_1");

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.image.restore_aspect_ratio"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof MusicControllerEditorElement));
            this.editor.history.saveSnapshot();
            for (AbstractEditorElement e : selectedElements) {
                ((MusicControllerElement)e.element).restoreAspectRatio();
            }
        }).setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

    }

    public MusicControllerElement getElement() {
        return (MusicControllerElement) this.element;
    }

}
