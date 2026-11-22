package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.InputStream;

/**
 * Loads the audio data of OpenAL audios like OGG and WAV in the background.<br><br>
 *
 * Reads the whole source stream into memory first and closes it right after that on every path, so the source file is never open
 * while its audio gets decoded, or after the audio got closed while it was loading. On Windows, an open file can't get moved or deleted,
 * and neither can the folders it is in.
 */
public final class ALAudioLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    private ALAudioLoader() {
    }

    /**
     * Loads the audio on a new thread. Takes ownership of the source stream and closes it.
     */
    public static void loadInBackground(@NotNull InputStream source, @NotNull String sourceName, @NotNull Target target) {
        FancyMenuThreads.startDaemonThread(() -> load(source, sourceName, target), "ALAudioLoader");
    }

    static void load(@NotNull InputStream source, @NotNull String sourceName, @NotNull Target target) {
        try {
            byte[] audioData;
            try (InputStream in = source) {
                audioData = in.readAllBytes();
            }
            if (!target.canContinueLoading()) return;
            ALAudioBuffer buffer = target.decode(audioData);
            // A buffer the audio didn't take is known to nobody else, so it has to get deleted here
            if (!target.attachDecodedBuffer(buffer)) buffer.deleteQuietly();
        } catch (Exception ex) {
            target.onLoadingFailed();
            LOGGER.error("[FANCYMENU] Failed to load audio: " + sourceName, ex);
        }
    }

    /**
     * The audio a background load runs for. All methods get called on the loading thread.
     */
    public interface Target {

        /**
         * @return False when loading should stop, like when the audio got closed or OpenAL became unusable.
         */
        boolean canContinueLoading();

        /**
         * Decodes the complete audio file.
         */
        @NotNull
        ALAudioBuffer decode(@NotNull byte[] audioData) throws Exception;

        /**
         * Attaches the decoded buffer to the audio, which then owns it and deletes it when it gets closed.
         *
         * @return False when the audio didn't take the buffer, like when it got closed while the buffer got decoded. The loader then deletes the buffer.
         */
        boolean attachDecodedBuffer(@NotNull ALAudioBuffer buffer);

        /**
         * Gets called when reading or decoding the audio failed.
         */
        void onLoadingFailed();

    }

}
