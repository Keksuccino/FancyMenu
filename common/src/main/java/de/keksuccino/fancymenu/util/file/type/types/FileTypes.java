package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class FileTypes {

    public static final FileType UNKNOWN = new FileType() {
        @Override
        public boolean isFileTypeLocal(@NotNull File file) {
            return true;
        }
        @Override
        public boolean isFileTypeWeb(@NotNull String fileUrl) {
            return true;
        }
    };

    public static final FileType JPEG_IMAGE = new FileType("image/jpeg", FileMediaType.IMAGE, "jpg", "jpeg");
    public static final FileType PNG_IMAGE = new FileType("image/png", FileMediaType.IMAGE, "png");
    public static final FileType GIF_IMAGE = new FileType("image/gif", FileMediaType.IMAGE, "gif");
    public static final FileType APNG_IMAGE = new FileType("image/apng", FileMediaType.IMAGE, "apng");

    public static final FileType OGG_AUDIO = new FileType("audio/ogg", FileMediaType.AUDIO, "ogg");
    public static final FileType MP3_AUDIO = new FileType("audio/mpeg", FileMediaType.AUDIO, "mp3");
    public static final FileType WAV_AUDIO = new FileType("audio/wav", FileMediaType.AUDIO, "wav");

    public static final FileType MPEG_VIDEO = new FileType("video/mpeg", FileMediaType.VIDEO, "mpeg", "mpg");
    public static final FileType MP4_VIDEO = new FileType("video/mp4", FileMediaType.VIDEO, "mp4");
    public static final FileType AVI_VIDEO = new FileType("video/x-msvideo", FileMediaType.VIDEO, "avi");

    public static final FileType TXT_TEXT = new FileType("text/plain", FileMediaType.TEXT, "txt");
    public static final FileType MARKDOWN_TEXT = new FileType("text/markdown", FileMediaType.TEXT, "md", "markdown");

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

    }

    @Nullable
    public static FileType getTypeOfLocalFile(@NotNull File file) {
        for (FileType type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeLocal(file)) return type;
        }
        return null;
    }

    @Nullable
    public static FileType getTypeOfWebFile(@NotNull String fileUrl) {
        for (FileType type : FileTypeRegistry.getFileTypes()) {
            if (type.isFileTypeWeb(fileUrl)) return type;
        }
        return null;
    }

}
