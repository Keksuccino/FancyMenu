package de.keksuccino.fancymenu.util.resources.audio;

import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resources.ResourceHandler;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected Map<String, IAudio> audios = new HashMap<>();

    @Override
    protected @NotNull Map<String, IAudio> getResourceMap() {
        return this.audios;
    }

    @Override
    public @NotNull List<AudioFileType> getAllowedFileTypes() {
        return FileTypes.getAllAudioFileTypes();
    }

}
