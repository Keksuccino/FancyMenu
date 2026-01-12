package de.keksuccino.fancymenu.customization.element.elements.cursor;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class CursorEditorElement extends AbstractEditorElement<CursorEditorElement, CursorElement> {

    public CursorEditorElement(@NotNull CursorElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericResourceChooserContextMenuEntryTo(this.rightClickMenu, "cursor_texture", CursorEditorElement.class,
                        () -> ResourceChooserScreen.image(null, file -> {}),
                        ResourceSupplier::image,
                        null,
                        consumes -> consumes.element.textureSupplier,
                        (cursorEditorElement, iTextureResourceSupplier) -> cursorEditorElement.element.textureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.cursor.cursor_texture"),
                        true, FileTypeGroup.of(FileTypes.PNG_IMAGE), null, true, true, false)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.cursor.cursor_texture.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("cursor"))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_cursor_texture").setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_x", CursorEditorElement.class,
                        consumes -> consumes.element.hotspotX,
                        (cursorEditorElement, integer) -> cursorEditorElement.element.hotspotX = integer,
                        Component.translatable("fancymenu.elements.cursor.hotspot_x"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.cursor.hotspot_x.desc")))
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_y", CursorEditorElement.class,
                        consumes -> consumes.element.hotspotY,
                        (cursorEditorElement, integer) -> cursorEditorElement.element.hotspotY = integer,
                        Component.translatable("fancymenu.elements.cursor.hotspot_y"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.cursor.hotspot_y.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_hotspot_y").setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "editor_preview_mode", CursorEditorElement.class,
                        consumes -> consumes.element.editorPreviewMode,
                        (cursorEditorElement, aBoolean) -> {
                            cursorEditorElement.element.editorPreviewMode = aBoolean;
                            cursorEditorElement.element.forceRebuildCursor();
                        },
                        "fancymenu.elements.cursor.editor_preview_mode")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.cursor.editor_preview_mode.desc")))
                .setStackable(true);

    }


}
