package de.keksuccino.fancymenu.customization.element.elements.cursor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CursorElementBuilder extends ElementBuilder<CursorElement, CursorEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public CursorElementBuilder() {
        super("cursor");
    }

    @Override
    public @NotNull CursorElement buildDefaultInstance() {
        CursorElement i = new CursorElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        return i;
    }

    @Override
    public CursorElement deserializeElement(@NotNull SerializedElement serialized) {

        CursorElement element = this.buildDefaultInstance();

        element.textureSupplier = deserializeImageResourceSupplier(serialized.getValue("source"));

        String editorPreviewMode = serialized.getValue("editor_preview_mode");
        if (editorPreviewMode != null) {
            if (editorPreviewMode.equals("true")) element.editorPreviewMode = true;
            if (editorPreviewMode.equals("false")) element.editorPreviewMode = false;
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull CursorElement element, @NotNull SerializedElement serializeTo) {

        if (element.textureSupplier != null) {
            serializeTo.putProperty("source", element.textureSupplier.getSourceWithPrefix());
        }

        serializeTo.putProperty("editor_preview_mode", "" + element.editorPreviewMode);

        return serializeTo;
        
    }

    @Override
    public @NotNull CursorEditorElement wrapIntoEditorElement(@NotNull CursorElement element, @NotNull LayoutEditorScreen editor) {
        return new CursorEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.cursor");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.cursor.desc");
    }

}
