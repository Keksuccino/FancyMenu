package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getAudioHandler()} instead.
 */
public class AudioResourceHandler extends ResourceHandler<IAudio, AudioFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getAudioHandler()} instead.
     */
    public static final AudioResourceHandler INSTANCE = new AudioResourceHandler();

    @Override
    public @NotNull List<AudioFileType> getAllowedFileTypes() {
        return FileTypes.getAllAudioFileTypes();
    }

    @Override
    public @Nullable AudioFileType getFallbackFileType() {
        return null;
    }

}
