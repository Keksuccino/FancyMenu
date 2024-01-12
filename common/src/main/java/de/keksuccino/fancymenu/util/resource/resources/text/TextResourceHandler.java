package de.keksuccino.fancymenu.util.resource.resources.text;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getTextHandler()} instead.
 */
public class TextResourceHandler extends ResourceHandler<IText, TextFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getTextHandler()} instead.
     */
    public static final TextResourceHandler INSTANCE = new TextResourceHandler();

    @Override
    public @NotNull List<TextFileType> getAllowedFileTypes() {
        return FileTypes.getAllTextFileTypes();
    }

    @Override
    public @Nullable TextFileType getFallbackFileType() {
        return FileTypes.TXT_TEXT;
    }

}
