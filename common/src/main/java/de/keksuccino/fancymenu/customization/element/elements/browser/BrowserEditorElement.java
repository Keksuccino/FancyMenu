package de.keksuccino.fancymenu.customization.element.elements.browser;

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

public class BrowserEditorElement extends AbstractEditorElement {

    public BrowserEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
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
                        BrowserEditorElement.class,
                        consumes -> consumes.getElement().repeat,
                        (browserEditorElement, aBoolean) -> browserEditorElement.getElement().repeat = aBoolean,
                        "fancymenu.elements.image.repeat")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().nineSlice)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_before_nine_slice_settings");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "nine_slice_texture",
                        BrowserEditorElement.class,
                        consumes -> consumes.getElement().nineSlice,
                        (browserEditorElement, aBoolean) -> browserEditorElement.getElement().nineSlice = aBoolean,
                        "fancymenu.elements.image.nine_slice")
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat)
                .setStackable(false);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_x",
                        BrowserEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderX,
                        (browserEditorElement, integer) -> browserEditorElement.getElement().nineSliceBorderX = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_x"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "nine_slice_border_y",
                        BrowserEditorElement.class,
                        consumes -> consumes.getElement().nineSliceBorderY,
                        (browserEditorElement, integer) -> browserEditorElement.getElement().nineSliceBorderY = integer,
                        Component.translatable("fancymenu.elements.image.nine_slice.border_y"), true, 5, null, null)
                .setStackable(false)
                .setIsActiveSupplier((menu, entry) -> !this.getElement().repeat);

        this.rightClickMenu.addSeparatorEntry("image_separator_1");

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.image.restore_aspect_ratio"), (menu, entry) -> {
                    List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof BrowserEditorElement));
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((BrowserElement)e.element).restoreAspectRatio();
                    }
                }).setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

    }

    public BrowserElement getElement() {
        return (BrowserElement) this.element;
    }

}
