package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.*;
import de.keksuccino.fancymenu.util.resources.audio.AudioResourceHandler;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.text.IText;
import de.keksuccino.fancymenu.util.resources.text.TextResourceHandler;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.ImageResourceHandler;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import de.keksuccino.fancymenu.util.resources.video.VideoResourceHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class ResourceHandlers {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected static ResourceHandler<ITexture, ImageFileType> imageHandler = ImageResourceHandler.INSTANCE;
    @NotNull
    protected static ResourceHandler<IAudio, AudioFileType> audioHandler = AudioResourceHandler.INSTANCE;
    @NotNull
    protected static ResourceHandler<IVideo, VideoFileType> videoHandler = VideoResourceHandler.INSTANCE;
    @NotNull
    protected static ResourceHandler<IText, TextFileType> textHandler = TextResourceHandler.INSTANCE;

    @NotNull
    public static ResourceHandler<ITexture, ImageFileType> getImageHandler() {
        return imageHandler;
    }

    public static void setImageHandler(@NotNull ResourceHandler<ITexture, ImageFileType> imageHandler) {
        ResourceHandlers.imageHandler = Objects.requireNonNull(imageHandler);
    }

    @NotNull
    public static ResourceHandler<IAudio, AudioFileType> getAudioHandler() {
        return audioHandler;
    }

    public static void setAudioHandler(@NotNull ResourceHandler<IAudio, AudioFileType> audioHandler) {
        ResourceHandlers.audioHandler = Objects.requireNonNull(audioHandler);
    }

    @NotNull
    public static ResourceHandler<IVideo, VideoFileType> getVideoHandler() {
        return videoHandler;
    }

    public static void setVideoHandler(@NotNull ResourceHandler<IVideo, VideoFileType> videoHandler) {
        ResourceHandlers.videoHandler = Objects.requireNonNull(videoHandler);
    }

    @NotNull
    public static ResourceHandler<IText, TextFileType> getTextHandler() {
        return textHandler;
    }

    public static void setTextHandler(@NotNull ResourceHandler<IText, TextFileType> textHandler) {
        ResourceHandlers.textHandler = Objects.requireNonNull(textHandler);
    }

    @NotNull
    public static List<ResourceHandler<?,?>> getHandlers() {
        return ListUtils.of(imageHandler, audioHandler, videoHandler, textHandler);
    }

    @Nullable
    public static ResourceHandler<?,?> findHandlerForSource(@NotNull ResourceSource source, boolean doAdvancedWebChecks) {
        FileType<?> type = FileTypes.getType(source, doAdvancedWebChecks);
        if (type instanceof ImageFileType) return getImageHandler();
        if (type instanceof AudioFileType) return getAudioHandler();
        if (type instanceof VideoFileType) return getVideoHandler();
        if (type instanceof TextFileType) return getTextHandler();
        return null;
    }

    public static void reloadAll() {
        LOGGER.info("[FANCYMENU] Reloading resources..");
        getHandlers().forEach(ResourceHandler::releaseAll);
    }

}
