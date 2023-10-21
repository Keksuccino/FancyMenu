package de.keksuccino.fancymenu.util.resources.texture;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.resources.ResourceHandler;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getImageHandler()} instead.
 */
@SuppressWarnings("unused")
public class ImageResourceHandler extends ResourceHandler<ITexture, ImageFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getImageHandler()} instead.
     */
    public static final ImageResourceHandler INSTANCE = new ImageResourceHandler();

    @Override
    public @NotNull List<ImageFileType> getAllowedFileTypes() {
        return FileTypes.getAllImageFileTypes();
    }

}
