package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AudioFileType extends FileType<IAudio> {

    public AudioFileType(@NotNull FileCodec<IAudio> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.AUDIO, extensions);
    }

    @Override
    public AudioFileType addExtension(@NotNull String extension) {
        return (AudioFileType) super.addExtension(extension);
    }

    @Override
    public AudioFileType removeExtension(@NotNull String extension) {
        return (AudioFileType) super.removeExtension(extension);
    }

    @Override
    public AudioFileType setCodec(@NotNull FileCodec<IAudio> codec) {
        return (AudioFileType) super.setCodec(codec);
    }

    @Override
    public AudioFileType setLocationAllowed(boolean allowLocation) {
        return (AudioFileType) super.setLocationAllowed(allowLocation);
    }

    @Override
    public AudioFileType setLocalAllowed(boolean allowLocal) {
        return (AudioFileType) super.setLocalAllowed(allowLocal);
    }

    @Override
    public AudioFileType setWebAllowed(boolean allowWeb) {
        return (AudioFileType) super.setWebAllowed(allowWeb);
    }

    @Override
    public AudioFileType setCustomDisplayName(@Nullable Component name) {
        return (AudioFileType) super.setCustomDisplayName(name);
    }

}
