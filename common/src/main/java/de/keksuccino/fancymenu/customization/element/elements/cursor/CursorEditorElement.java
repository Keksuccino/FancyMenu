package de.keksuccino.fancymenu.customization.element.elements.cursor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class CursorEditorElement extends AbstractEditorElement {

    public CursorEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericResourceChooserContextMenuEntryTo(this.rightClickMenu, "cursor_texture", CursorEditorElement.class,
                        () -> ResourceChooserScreen.image(null, file -> {}),
                        ResourceSupplier::image,
                        null,
                        consumes -> consumes.getElement().textureSupplier,
                        (cursorEditorElement, iTextureResourceSupplier) -> cursorEditorElement.getElement().textureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.customization.elements.cursor.cursor_texture"),
                        true, FileTypeGroup.of(FileTypes.PNG_IMAGE), null, true, true, false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.cursor_texture.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("cursor"))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_cursor_texture").setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_x", CursorEditorElement.class,
                        consumes -> consumes.getElement().hotspotX,
                        (cursorEditorElement, integer) -> cursorEditorElement.getElement().hotspotX = integer,
                        Component.translatable("fancymenu.customization.elements.cursor.hotspot_x"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.hotspot_x.desc")))
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_y", CursorEditorElement.class,
                        consumes -> consumes.getElement().hotspotY,
                        (cursorEditorElement, integer) -> cursorEditorElement.getElement().hotspotY = integer,
                        Component.translatable("fancymenu.customization.elements.cursor.hotspot_y"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.hotspot_y.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_hotspot_y").setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "editor_preview_mode", CursorEditorElement.class,
                        consumes -> consumes.getElement().editorPreviewMode,
                        (cursorEditorElement, aBoolean) -> {
                            cursorEditorElement.getElement().editorPreviewMode = aBoolean;
                            cursorEditorElement.getElement().forceRebuildCursor();
                        },
                        "fancymenu.customization.elements.cursor.editor_preview_mode")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.editor_preview_mode.desc")))
                .setStackable(true);

    }

    public CursorElement getElement() {
        return (CursorElement) this.element;
    }

}
