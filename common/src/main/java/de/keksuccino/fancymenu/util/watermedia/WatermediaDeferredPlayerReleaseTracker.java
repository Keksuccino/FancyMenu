package de.keksuccino.fancymenu.util.watermedia;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Owns Watermedia players detached from closed MP4 resources while their normal render-thread release is deferred.
 * Shutdown permanently closes registration before taking the snapshot, so a player cannot be lost between final screen removal and queue shutdown.
 */
public final class WatermediaDeferredPlayerReleaseTracker {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object LIFECYCLE_LOCK = new Object();
    private static final Set<Object> PLAYERS = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean shuttingDown;

    private WatermediaDeferredPlayerReleaseTracker() {}

    public static boolean track(@NotNull Object player) {
        synchronized (LIFECYCLE_LOCK) {
            if (shuttingDown) return false;
            PLAYERS.add(player);
            return true;
        }
    }

    public static void release(@NotNull Object player) {
        synchronized (LIFECYCLE_LOCK) {
            if (!PLAYERS.remove(player)) return;
        }
        WatermediaReflectionBridge.playerRelease(player);
    }

    public static void shutdown() {
        List<Object> playersToRelease;
        synchronized (LIFECYCLE_LOCK) {
            if (shuttingDown) return;
            shuttingDown = true;
            playersToRelease = new ArrayList<>(PLAYERS);
            PLAYERS.clear();
        }
        for (Object player : playersToRelease) {
            try {
                WatermediaReflectionBridge.playerStop(player);
                WatermediaReflectionBridge.playerReleaseForShutdown(player);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Failed to release a deferred Watermedia player during client shutdown!", throwable);
            }
        }
    }
}
