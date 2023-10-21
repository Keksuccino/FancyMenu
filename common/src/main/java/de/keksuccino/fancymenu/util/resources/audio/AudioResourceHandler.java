package de.keksuccino.fancymenu.util.resources.audio;

import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resources.ResourceHandler;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
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

}
