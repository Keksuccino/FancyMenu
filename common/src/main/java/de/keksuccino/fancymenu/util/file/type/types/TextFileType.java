package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.resources.text.IText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextFileType extends FileType<IText> {

    public TextFileType(@NotNull FileCodec<IText> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.TEXT, extensions);
    }

}
