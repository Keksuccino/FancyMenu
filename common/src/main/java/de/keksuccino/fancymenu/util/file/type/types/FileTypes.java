package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.texture.*;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTypes {

    //TODO Full APNG support adden

    //TODO TextureManager texture type identification durch getAllForMediaType() ersetzen

    //TODO TextureManager texture instance construction über FileTypes regeln

    public static final FileType<Object> UNKNOWN = new FileType<>(FileCodec.basic(Object.class, consumes -> null)) {
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
            FileCodec.advanced(ITexture.class, SimpleTexture::of, SimpleTexture::local, SimpleTexture::web),
            "image/jpeg", "jpg", "jpeg");
    public static final ImageFileType PNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, SimpleTexture::of, SimpleTexture::local, SimpleTexture::web),
            "image/png", "png");
    public static final ImageFileType GIF_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, GifTexture::of, GifTexture::local, GifTexture::web),
            "image/gif", "gif");
    public static final ImageFileType APNG_IMAGE = new ImageFileType(
            FileCodec.advanced(ITexture.class, ApngTexture::of, ApngTexture::local, ApngTexture::web),
            "image/apng", "apng");

    //TODO implement audio codecs (and remove file types that have no codec)
    public static final AudioFileType OGG_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null), "audio/ogg", "ogg");
    public static final AudioFileType MP3_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null), "audio/mpeg", "mp3");
    public static final AudioFileType WAV_AUDIO = new AudioFileType(FileCodec.basic(IAudio.class, consumes -> null), "audio/wav", "wav");

    //TODO implement video codecs (and remove file types that have no codec)
    public static final VideoFileType MPEG_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null), "video/mpeg", "mpeg", "mpg");
    public static final VideoFileType MP4_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null), "video/mp4", "mp4");
    public static final VideoFileType AVI_VIDEO = new VideoFileType(FileCodec.basic(IVideo.class, consumes -> null), "video/x-msvideo", "avi");

    private static final FileCodec<TextFileType.PlainText> PLAIN_TEXT_CODEC = FileCodec.basic(
            TextFileType.PlainText.class, consumes -> new TextFileType.PlainText(FileUtils.readTextLinesFrom(consumes)));

    public static final TextFileType TXT_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/plain", "txt");
    public static final TextFileType MARKDOWN_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/markdown", "md", "markdown");
    public static final TextFileType JSON_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "application/json", "json");
    public static final TextFileType LOG_TEXT = new TextFileType(PLAIN_TEXT_CODEC, null, "log");
    public static final TextFileType LANG_TEXT = new TextFileType(PLAIN_TEXT_CODEC, null, "lang");
    public static final TextFileType LOCAL_TEXT = new TextFileType(PLAIN_TEXT_CODEC, null, "local");
    public static final TextFileType PROPERTIES_TEXT = new TextFileType(PLAIN_TEXT_CODEC, null, "properties");
    public static final TextFileType XML_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "application/xml", "xml");
    public static final TextFileType JAVASCRIPT_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/javascript", "js");
    public static final TextFileType HTML_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/html", "htm", "html", "shtml");
    public static final TextFileType CSS_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/css", "css");
    public static final TextFileType CSV_TEXT = new TextFileType(PLAIN_TEXT_CODEC, "text/csv", "csv");

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

    @NotNull
    public static List<ImageFileType> getAllImageFileTypes() {
        List<ImageFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof ImageFileType i) types.add(i);
        }
        return types;
    }

    @NotNull
    public static List<AudioFileType> getAllAudioFileTypes() {
        List<AudioFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof AudioFileType a) types.add(a);
        }
        return types;
    }

    @NotNull
    public static List<VideoFileType> getAllVideoFileTypes() {
        List<VideoFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof VideoFileType v) types.add(v);
        }
        return types;
    }

    @NotNull
    public static List<TextFileType> getAllTextFileTypes() {
        List<TextFileType> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type instanceof TextFileType t) types.add(t);
        }
        return types;
    }

    @NotNull
    public static List<FileType<?>> getAllForMediaType(@NotNull FileMediaType mediaType) {
        List<FileType<?>> types = new ArrayList<>();
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.getMediaType() == mediaType) types.add(type);
        }
        return types;
    }

    @Nullable
    public static FileType<?> getTypeOfLocalFile(@NotNull File file) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocal(file)) return type;
        }
        return null;
    }

    @Nullable
    public static FileType<?> getTypeOfWebFile(@NotNull String fileUrl) {
        for (FileType<?> type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeWeb(fileUrl)) return type;
        }
        return null;
    }

}
