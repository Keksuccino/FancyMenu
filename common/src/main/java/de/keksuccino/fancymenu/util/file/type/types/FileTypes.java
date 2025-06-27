package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.ogg.OggAudio;
import de.keksuccino.fancymenu.util.resource.resources.audio.wav.WavAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.text.PlainText;
import de.keksuccino.fancymenu.util.resource.resources.texture.*;
import de.keksuccino.fancymenu.util.resource.resources.texture.fma.FmaTexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileTypes {

    public static final FileType<Object> UNKNOWN = new FileType<>(FileCodec.empty(Object.class)) {
        @Override
        public boolean isFileTypeLocal(@NotNull File file) {
            return true;
        }
        @Override
        public boolean isFileTypeWeb(@NotNull String fileUrl) {
            return true;
        }
    };

    public static final ImageFileType JPEG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, JpegTexture::of, JpegTexture::location, JpegTexture::local, JpegTexture::web),
            "image/jpeg", "jpg", "jpeg");
    public static final ImageFileType PNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, PngTexture::of, PngTexture::location, PngTexture::local, PngTexture::web),
            "image/png", "png");
    public static final ImageFileType GIF_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, GifTexture::of, GifTexture::location, GifTexture::local, GifTexture::web),
            "image/gif", "gif")
            .setAnimated(true);
    public static final ImageFileType APNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, ApngTexture::of, ApngTexture::location, ApngTexture::local, ApngTexture::web),
            "image/apng", "apng")
            .setCustomDisplayName(Component.translatable("fancymenu.file_types.apng"))
            .setAnimated(true);
    public static final ImageFileType FMA_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, FmaTexture::of, FmaTexture::location, FmaTexture::local, FmaTexture::web),
            "image/fma", "fma")
            .setCustomDisplayName(Component.translatable("fancymenu.file_types.fma"))
            .setAnimated(true);

    public static final AudioFileType OGG_AUDIO = new AudioFileType(
            FileCodec.advanced(IAudio.class, OggAudio::of, OggAudio::location, OggAudio::local, OggAudio::web),
            "audio/ogg", "ogg");
    public static final AudioFileType WAV_AUDIO = new AudioFileType(
            FileCodec.advanced(IAudio.class, WavAudio::of, WavAudio::location, WavAudio::local, WavAudio::web),
            "audio/wav", "wav");

//    public static final VideoFileType MPEG_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/mpeg", "mpeg", "mpg");
    public static final VideoFileType MP4_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/mp4", "mp4");
//    public static final VideoFileType AVI_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/x-msvideo", "avi");

    public static final TextFileType TXT_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/plain", "txt");
    public static final TextFileType MARKDOWN_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/markdown", "md", "markdown");
    public static final TextFileType JSON_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "application/json", "json");
    public static final TextFileType LOG_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            null, "log");
    public static final TextFileType LANG_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            null, "lang");
    public static final TextFileType LOCAL_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            null, "local");
    public static final TextFileType PROPERTIES_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            null, "properties");
    public static final TextFileType XML_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "application/xml", "xml");
    public static final TextFileType JAVASCRIPT_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/javascript", "js");
    public static final TextFileType HTML_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/html", "htm", "html", "shtml");
    public static final TextFileType CSS_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/css", "css");
    public static final TextFileType CSV_TEXT = new TextFileType(
            FileCodec.advanced(IText.class, PlainText::of, PlainText::location, PlainText::local, PlainText::web),
            "text/csv", "csv");

    public static void registerAll() {

        FileTypeRegistry.register("jpeg", JPEG_IMAGE);
        FileTypeRegistry.register("png", PNG_IMAGE);
        FileTypeRegistry.register("gif", GIF_IMAGE);
        FileTypeRegistry.register("apng", APNG_IMAGE);
        FileTypeRegistry.register("fma", FMA_IMAGE);

        FileTypeRegistry.register("ogg", OGG_AUDIO);
        FileTypeRegistry.register("wav", WAV_AUDIO);

//        FileTypeRegistry.register("mpeg", MPEG_VIDEO);
        FileTypeRegistry.register("mp4", MP4_VIDEO);
//        FileTypeRegistry.register("avi", AVI_VIDEO);

        FileTypeRegistry.register("txt", TXT_TEXT);
        FileTypeRegistry.register("markdown", MARKDOWN_TEXT);
        FileTypeRegistry.register("json", JSON_TEXT);
        FileTypeRegistry.register("log", LOG_TEXT);
        FileTypeRegistry.register("lang", LANG_TEXT);
        FileTypeRegistry.register("local", LOCAL_TEXT);
        FileTypeRegistry.register("properties", PROPERTIES_TEXT);
        FileTypeRegistry.register("xml", XML_TEXT);
        FileTypeRegistry.register("js", JAVASCRIPT_TEXT);
        FileTypeRegistry.register("html", HTML_TEXT);
        FileTypeRegistry.register("css", CSS_TEXT);
        FileTypeRegistry.register("csv", CSV_TEXT);

    }

    /**
     * Returns all {@link ImageFileType}s registered in the {@link FileTypeRegistry}.<br>
     * Default types listed in {@link FileTypes} are included.
     */
    @NotNull
    public static List<ImageFileType> getAllImageFileTypes() {
        List<ImageFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof ImageFileType i) types.add(i);
        }
        return types;
    }

    /**
     * Returns all animated {@link ImageFileType}s registered in the {@link FileTypeRegistry}.<br>
     * Default types listed in {@link FileTypes} are included.
     */
    @NotNull
    public static List<ImageFileType> getAllAnimatedImageFileTypes() {
        List<ImageFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if ((type instanceof ImageFileType i) && i.isAnimated()) types.add(i);
        }
        return types;
    }

    /**
     * Returns all {@link AudioFileType}s registered in the {@link FileTypeRegistry}.<br>
     * Default types listed in {@link FileTypes} are included.
     */
    @NotNull
    public static List<AudioFileType> getAllAudioFileTypes() {
        List<AudioFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof AudioFileType a) types.add(a);
        }
        return types;
    }

    /**
     * Returns all {@link VideoFileType}s registered in the {@link FileTypeRegistry}.<br>
     * Default types listed in {@link FileTypes} are included.
     */
    @NotNull
    public static List<VideoFileType> getAllVideoFileTypes() {
        List<VideoFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof VideoFileType v) types.add(v);
        }
        return types;
    }

    /**
     * Returns all {@link TextFileType}s registered in the {@link FileTypeRegistry}.<br>
     * Default types listed in {@link FileTypes} are included.
     */
    @NotNull
    public static List<TextFileType> getAllTextFileTypes() {
        List<TextFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof TextFileType t) types.add(t);
        }
        return types;
    }

    /**
     * Tries to find the {@link FileType} of a {@link ResourceLocation} file.
     */
    @Nullable
    public static FileType<?> getLocationType(@NotNull ResourceLocation location) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocation(location)) return type;
        }
        return null;
    }

    /**
     * Tries to find the {@link FileType} of a local file.
     */
    @Nullable
    public static FileType<?> getLocalType(@NotNull File file) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocal(file)) return type;
        }
        return null;
    }

    /**
     * Tries to find the {@link FileType} of a web file.
     */
    @Nullable
    public static FileType<?> getWebType(@NotNull String fileUrl, boolean doAdvancedWebChecks) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeWeb(fileUrl)) return type;
        }
        if (doAdvancedWebChecks) {
            for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
                if (type.isFileTypeWebAdvanced(fileUrl)) return type;
            }
        }
        return null;
    }

    /**
     * Tries to find the {@link FileType} of a resource source.<br>
     * Resource sources can be web URLs, local paths or {@link ResourceLocation}s (namespace:path).
     */
    @Nullable
    public static FileType<?> getType(@NotNull ResourceSource resourceSource, boolean doAdvancedWebChecks) {
        Objects.requireNonNull(resourceSource);
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileType(resourceSource, false)) return type;
        }
        if (doAdvancedWebChecks && (resourceSource.getSourceType() == ResourceSourceType.WEB)) {
            for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
                if (type.isFileTypeWebAdvanced(resourceSource.getSourceWithoutPrefix())) return type;
            }
        }
        return null;
    }

}
