package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import de.keksuccino.fancymenu.util.resources.texture.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTypes {

    //TODO Full APNG support adden

    //TODO TextureManager texture type identification durch getAllForMediaType() ersetzen

    //TODO TextureManager texture instance construction über FileTypes regeln

    public static final FileType<Object> UNKNOWN = new FileType<>() {
        @Override
        public boolean isFileTypeLocal(@NotNull File file) {
            return true;
        }
        @Override
        public boolean isFileTypeWeb(@NotNull String fileUrl) {
            return true;
        }
    };

    public static final FileType<ITexture> JPEG_IMAGE = new FileType<ITexture>("image/jpeg", FileMediaType.IMAGE, "jpg", "jpeg")
            .setLocalCodec(LocalTexture::of)
            .setWebCodec(SimpleWebTexture::of);
    public static final FileType<ITexture> PNG_IMAGE = new FileType<ITexture>("image/png", FileMediaType.IMAGE, "png")
            .setLocalCodec(LocalTexture::of)
            .setWebCodec(SimpleWebTexture::of);
    public static final FileType<ITexture> GIF_IMAGE = new FileType<ITexture>("image/gif", FileMediaType.IMAGE, "gif")
            .setLocalCodec(consumes -> GifTexture.local(new File(consumes)))
            .setWebCodec(GifTexture::web);
    public static final FileType<ITexture> APNG_IMAGE = new FileType<ITexture>("image/apng", FileMediaType.IMAGE, "apng")
            .setLocalCodec(consumes -> ApngTexture.local(new File(consumes)))
            .setWebCodec(ApngTexture::web);

    public static final FileType<Object> OGG_AUDIO = new FileType<>("audio/ogg", FileMediaType.AUDIO, "ogg");
    public static final FileType<Object> MP3_AUDIO = new FileType<>("audio/mpeg", FileMediaType.AUDIO, "mp3");
    public static final FileType<Object> WAV_AUDIO = new FileType<>("audio/wav", FileMediaType.AUDIO, "wav");

    public static final FileType<Object> MPEG_VIDEO = new FileType<>("video/mpeg", FileMediaType.VIDEO, "mpeg", "mpg");
    public static final FileType<Object> MP4_VIDEO = new FileType<>("video/mp4", FileMediaType.VIDEO, "mp4");
    public static final FileType<Object> AVI_VIDEO = new FileType<>("video/x-msvideo", FileMediaType.VIDEO, "avi");

    public static final FileType<List<String>> TXT_TEXT = new FileType<>("text/plain", FileMediaType.TEXT, "txt");
    public static final FileType<List<String>> MARKDOWN_TEXT = new FileType<>("text/markdown", FileMediaType.TEXT, "md", "markdown");

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
