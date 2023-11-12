package de.keksuccino.fancymenu.util.resources.audio.ogg.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioFormat;

public class OpenALUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static String getOpenAlError() {
        int errorResult = AL10.alGetError();
        return (errorResult != 0) ? AL10.alGetString(errorResult) : null;
    }

    public static boolean checkAndPrintOpenAlError(@NotNull String actionDescription) {
        return checkAndPrintOpenAlError(actionDescription, null);
    }

    public static boolean checkAndPrintOpenAlError(@NotNull String actionDescription, @Nullable Exception exception) {
        String error = getOpenAlError();
        if (error != null) {
            if (exception == null) exception = new Exception();
            LOGGER.error("[FANCYMENU] Error while handling audio: ACTION: " + actionDescription + " | ERROR: " + error, exception);
            return true;
        }
        return false;
    }

    public static int getAudioFormatAsOpenAL(@NotNull AudioFormat audioFormat) throws IllegalArgumentException {
        AudioFormat.Encoding encoding = audioFormat.getEncoding();
        int channels = audioFormat.getChannels();
        int sampleSize = audioFormat.getSampleSizeInBits();
        if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED) || encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSize == 8) {
                    return AL10.AL_FORMAT_MONO8;
                }
                if (sampleSize == 16) {
                    return AL10.AL_FORMAT_MONO16;
                }
            } else if (channels == 2) {
                if (sampleSize == 8) {
                    return AL10.AL_FORMAT_STEREO8;
                }
                if (sampleSize == 16) {
                    return AL10.AL_FORMAT_STEREO16;
                }
            }
        }
        throw new IllegalArgumentException("Failed to convert AudioFormat to OpenAL! Unsupported format: " + audioFormat);
    }

}
