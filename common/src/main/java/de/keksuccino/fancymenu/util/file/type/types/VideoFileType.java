package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VideoFileType extends FileType<IVideo> {

    public VideoFileType(@NotNull FileCodec<IVideo> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.VIDEO, extensions);
    }

    @Override
    public VideoFileType addExtension(@NotNull String extension) {
        return (VideoFileType) super.addExtension(extension);
    }

    @Override
    public VideoFileType removeExtension(@NotNull String extension) {
        return (VideoFileType) super.removeExtension(extension);
    }

    @Override
    public VideoFileType setCodec(@NotNull FileCodec<IVideo> codec) {
        return (VideoFileType) super.setCodec(codec);
    }

    @Override
    public VideoFileType setLocationAllowed(boolean allowLocation) {
        return (VideoFileType) super.setLocationAllowed(allowLocation);
    }

    @Override
    public VideoFileType setLocalAllowed(boolean allowLocal) {
        return (VideoFileType) super.setLocalAllowed(allowLocal);
    }

    @Override
    public VideoFileType setWebAllowed(boolean allowWeb) {
        return (VideoFileType) super.setWebAllowed(allowWeb);
    }

    @Override
    public VideoFileType setCustomDisplayName(@Nullable Component name) {
        return (VideoFileType) super.setCustomDisplayName(name);
    }

}
