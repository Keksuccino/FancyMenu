package de.keksuccino.fancymenu.util.resource.resources.video;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getVideoHandler()} instead.
 */
public class VideoResourceHandler extends ResourceHandler<IVideo, VideoFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getVideoHandler()} instead.
     */
    public static final VideoResourceHandler INSTANCE = new VideoResourceHandler();

    @Override
    public @NotNull List<VideoFileType> getAllowedFileTypes() {
        return FileTypes.getAllVideoFileTypes();
    }

    @Override
    public @Nullable VideoFileType getFallbackFileType() {
        return null;
    }

}
