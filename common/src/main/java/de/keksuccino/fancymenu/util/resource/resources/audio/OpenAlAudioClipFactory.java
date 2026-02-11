package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import java.lang.reflect.Constructor;

public final class OpenAlAudioClipFactory {

    private static final long INITIAL_RETRY_DELAY_MS = 400L;
    private static final long MAX_RETRY_DELAY_MS = 2000L;
    private static Constructor<ALAudioClip> cachedClipConstructor;
    private static volatile long nextCreationAttemptMs = 0L;
    private static volatile long retryDelayMs = INITIAL_RETRY_DELAY_MS;

    private OpenAlAudioClipFactory() {
    }

    public static boolean isCreationTemporarilyBlocked() {
        return System.currentTimeMillis() < nextCreationAttemptMs;
    }

    @Nullable
    public static ALAudioClip createSafe() {
        long now = System.currentTimeMillis();
        if (now < nextCreationAttemptMs) {
            return null;
        }
        if (!ALUtils.isOpenAlReady() || AudioEngineReloadHandler.isInPostReloadCooldown()) {
            return null;
        }
        int source = 0;
        try {
            int[] sourceArr = new int[1];
            AL10.alGenSources(sourceArr);
            ALErrorHandler.checkOpenAlError();
            source = sourceArr[0];
            if (source == 0) {
                scheduleRetry(now);
                return null;
            }
            ALAudioClip clip = getClipConstructor().newInstance(source);
            resetRetryBackoff();
            return clip;
        } catch (Exception ex) {
            scheduleRetry(now);
            tryDeleteSourceQuietly(source);
        }
        return null;
    }

    private static synchronized void scheduleRetry(long now) {
        long delay = retryDelayMs;
        nextCreationAttemptMs = Math.max(nextCreationAttemptMs, now + delay);
        retryDelayMs = Math.min(MAX_RETRY_DELAY_MS, delay * 2L);
    }

    private static synchronized void resetRetryBackoff() {
        nextCreationAttemptMs = 0L;
        retryDelayMs = INITIAL_RETRY_DELAY_MS;
    }

    private static Constructor<ALAudioClip> getClipConstructor() throws Exception {
        Constructor<ALAudioClip> ctor = cachedClipConstructor;
        if (ctor == null) {
            ctor = ALAudioClip.class.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            cachedClipConstructor = ctor;
        }
        return ctor;
    }

    private static void tryDeleteSourceQuietly(int source) {
        if (source == 0) return;
        try {
            if (AL10.alIsSource(source)) {
                AL10.alDeleteSources(new int[]{source});
                ALErrorHandler.getOpenAlError();
            }
        } catch (Exception ignored) {
        }
    }
}
