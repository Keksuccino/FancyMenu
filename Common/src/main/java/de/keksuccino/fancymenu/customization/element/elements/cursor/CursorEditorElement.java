package de.keksuccino.fancymenu.customization.element.elements.cursor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
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

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "cursor_texture", CursorEditorElement.class,
                        null,
                        consumes -> consumes.getCursorElement().source,
                        (cursorEditorElement, s) -> cursorEditorElement.getCursorElement().source = s,
                        Component.translatable("fancymenu.customization.elements.cursor.cursor_texture"),
                        true, file -> file.getAbsolutePath().toLowerCase().endsWith(".png"))
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.cursor_texture.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_cursor_texture").setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_x", CursorEditorElement.class,
                        consumes -> consumes.getCursorElement().hotspotX,
                        (cursorEditorElement, integer) -> cursorEditorElement.getCursorElement().hotspotX = integer,
                        Component.translatable("fancymenu.customization.elements.cursor.hotspot_x"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.hotspot_x.desc")))
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "hotspot_y", CursorEditorElement.class,
                        consumes -> consumes.getCursorElement().hotspotY,
                        (cursorEditorElement, integer) -> cursorEditorElement.getCursorElement().hotspotY = integer,
                        Component.translatable("fancymenu.customization.elements.cursor.hotspot_y"),
                        true, 0, MathUtils::isInteger, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.hotspot_y.desc")))
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_hotspot_y").setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "editor_preview_mode", CursorEditorElement.class,
                        consumes -> consumes.getCursorElement().editorPreviewMode,
                        (cursorEditorElement, aBoolean) -> {
                            cursorEditorElement.getCursorElement().editorPreviewMode = aBoolean;
                            cursorEditorElement.getCursorElement().forceRebuildCursor();
                        },
                        "fancymenu.customization.elements.cursor.editor_preview_mode")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.elements.cursor.editor_preview_mode.desc")))
                .setStackable(true);

    }

    public CursorElement getCursorElement() {
        return (CursorElement) this.element;
    }

}
