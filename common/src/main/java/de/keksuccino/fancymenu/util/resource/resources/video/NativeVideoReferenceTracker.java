package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Tracks active references for shared native video resources.
 */
public final class NativeVideoReferenceTracker {

    private static final Object LOCK = new Object();
    private static final Map<IVideo, Integer> REFERENCE_COUNTS = new IdentityHashMap<>();

    private NativeVideoReferenceTracker() {
    }

    public static void acquire(@NotNull IVideo video) {
        synchronized (LOCK) {
            REFERENCE_COUNTS.merge(video, 1, Integer::sum);
        }
    }

    /**
     * @return {@code true} if this was the last known reference and the caller should stop/release it.
     */
    public static boolean release(@NotNull IVideo video) {
        synchronized (LOCK) {
            Integer count = REFERENCE_COUNTS.get(video);
            if ((count == null) || (count <= 1)) {
                REFERENCE_COUNTS.remove(video);
                return true;
            }
            REFERENCE_COUNTS.put(video, count - 1);
            return false;
        }
    }

    public static boolean hasReferences(@NotNull IVideo video) {
        synchronized (LOCK) {
            Integer count = REFERENCE_COUNTS.get(video);
            return (count != null) && (count > 0);
        }
    }
}
