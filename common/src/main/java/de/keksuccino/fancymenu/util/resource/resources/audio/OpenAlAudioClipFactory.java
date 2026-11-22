package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import java.lang.reflect.Constructor;

public final class OpenAlAudioClipFactory {

    private static Constructor<ALAudioClip> cachedClipConstructor;

    private OpenAlAudioClipFactory() {
    }

    @Nullable
    public static ALAudioClip createSafe() {
        if (!ALUtils.isOpenAlReady()) {
            return null;
        }
        int source = 0;
        try {
            int[] sourceArr = new int[1];
            AL10.alGenSources(sourceArr);
            // Gets read before the error check, because OpenAL errors stay pending until they get read, so the check can also fail
            // because of an older error after the source got made, and the source has to get deleted then
            source = sourceArr[0];
            ALErrorHandler.checkOpenAlError();
            if (source == 0) {
                return null;
            }
            return getClipConstructor().newInstance(source);
        } catch (Exception ex) {
            tryDeleteSourceQuietly(source);
        }
        return null;
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
