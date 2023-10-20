package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.text.IText;
import de.keksuccino.fancymenu.util.resources.text.PlainText;
import de.keksuccino.fancymenu.util.resources.texture.*;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            FileCodec.advanced(ITexture.class, SimpleTexture::of, SimpleTexture::location, SimpleTexture::local, SimpleTexture::web),
            "image/jpeg", "jpg", "jpeg");
    public static final ImageFileType PNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, SimpleTexture::of, SimpleTexture::location, SimpleTexture::local, SimpleTexture::web),
            "image/png", "png");
    public static final ImageFileType GIF_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, GifTexture::of, GifTexture::location, GifTexture::local, GifTexture::web),
            "image/gif", "gif");
    public static final ImageFileType APNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, ApngTexture::of, ApngTexture::location, ApngTexture::local, ApngTexture::web),
            "image/apng", "apng").setCustomDisplayName(Component.translatable("fancymenu.file_types.apng"));

    //TODO implement audio codecs (and remove file types that have no codec)
    public static final AudioFileType OGG_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null, consumes -> null), "audio/ogg", "ogg");
    public static final AudioFileType MP3_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null, consumes -> null), "audio/mpeg", "mp3");
    public static final AudioFileType WAV_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null, consumes -> null), "audio/wav", "wav");

    //TODO implement video codecs (and remove file types that have no codec)
    public static final VideoFileType MPEG_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/mpeg", "mpeg", "mpg");
    public static final VideoFileType MP4_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/mp4", "mp4");
    public static final VideoFileType AVI_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null, consumes -> null), "video/x-msvideo", "avi");

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

        FileTypeRegistry.register("ogg", OGG_AUDIO);
        FileTypeRegistry.register("mp3", MP3_AUDIO);
        FileTypeRegistry.register("wav", WAV_AUDIO);

        FileTypeRegistry.register("mpeg", MPEG_VIDEO);
        FileTypeRegistry.register("mp4", MP4_VIDEO);
        FileTypeRegistry.register("avi", AVI_VIDEO);

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
    public static FileType<?> getTypeOfLocationFile(@NotNull ResourceLocation location) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocation(location)) return type;
        }
        return null;
    }

    /**
     * Tries to find the {@link FileType} of a local file.
     */
    @Nullable
    public static FileType<?> getTypeOfLocalFile(@NotNull File file) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocal(file)) return type;
        }
        return null;
    }

    /**
     * Tries to find the {@link FileType} of a web file.
     */
    @Nullable
    public static FileType<?> getTypeOfWebFile(@NotNull String fileUrl) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeWeb(fileUrl)) return type;
        }
        return null;
    }

}
