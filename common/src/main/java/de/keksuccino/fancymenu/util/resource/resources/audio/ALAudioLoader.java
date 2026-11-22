package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;

/**
 * Opens the sources of OpenAL audios like OGG and WAV and loads their audio data in the background.<br><br>
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
     * Opens the source stream of an audio. When the source is missing or can't get opened, closes the pending clip,
     * runs the failure callback and gives null.
     *
     * @param pendingClip The clip that got created for the audio before its source got opened, like for web sources, whose
     *                    streams get opened in the background while clips need to get created on the render thread. Null when
     *                    the clip gets created only after the source got opened.
     * @param onFailed Marks the load of the audio as failed.
     */
    @Nullable
    public static InputStream openSource(@NotNull SourceOpener opener, @NotNull String sourceName, @Nullable ALAudioClip pendingClip, @NotNull Runnable onFailed) {
        try {
            InputStream in = opener.open();
            if (in != null) return in;
            LOGGER.error("[FANCYMENU] Failed to open audio source: " + sourceName);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to open audio source: " + sourceName, ex);
        }
        // No audio took the clip yet, so nobody else would ever close it
        CloseableUtils.closeQuietly(pendingClip);
        onFailed.run();
        return null;
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
            boolean attached = false;
            try {
                attached = target.attachDecodedBuffer(buffer);
            } finally {
                // A buffer the audio didn't take is known to nobody else, so it has to get deleted here
                if (!attached) buffer.deleteQuietly();
            }
        } catch (Exception ex) {
            target.onLoadingFailed();
            LOGGER.error("[FANCYMENU] Failed to load audio: " + sourceName, ex);
        }
    }

    /**
     * Opens the source stream of an audio.
     */
    @FunctionalInterface
    public interface SourceOpener {

        /**
         * @return The opened source stream, or null when the source can't get opened.
         */
        @Nullable
        InputStream open() throws Exception;

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
         * Gets called when reading, decoding or attaching the audio failed.
         */
        void onLoadingFailed();

    }

}
