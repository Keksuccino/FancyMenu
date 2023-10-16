package de.keksuccino.fancymenu.util.resources.video;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resources.ResourceHandler;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected Map<String, IVideo> videos = new HashMap<>();

    @Override
    protected @NotNull Map<String, IVideo> getResourceMap() {
        return this.videos;
    }

    @Override
    public @NotNull List<VideoFileType> getAllowedFileTypes() {
        return FileTypes.getAllVideoFileTypes();
    }

}
