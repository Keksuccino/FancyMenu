package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.melody.resources.audio.MinecraftSoundSettingsObserver;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<String, Pair<WrappedRinkuBrowser, Long>> BROWSERS = new HashMap<>();

    @Nullable private static volatile Long volumeListenerId = null;
    private static volatile boolean shuttingDown = false;
    private static volatile boolean is_initializing = false;
    public static volatile boolean initialized = false;

    public static void init() {
        synchronized (BrowserHandler.class) {
            if (shuttingDown || initialized || is_initializing) return;
            is_initializing = true;
        }

        LOGGER.info("[FANCYMENU] Starting initialization of BrowserHandler..");

        if (Rinku.isInitialized()) {
            completeInitialization(true);
            return;
        }

        LOGGER.warn("[FANCYMENU] Rinku not initialized yet! Registering FancyMenu's browser integration for Rinku's initialization phase.");

        // Rinku invokes these callbacks after creating its client but before creating its preloaded browsers. The
        // message router must exist before those browser contexts or the first pooled browser never receives cefQuery.
        Rinku.scheduleForInit(BrowserHandler::completeInitialization);

        // Rinku and both loader startup paths initialize on the client thread. Keep an idempotent recheck as a defensive
        // fallback for integrations that report Rinku ready immediately after listener registration.
        if (Rinku.isInitialized()) completeInitialization(true);

    }

    private static synchronized void completeInitialization(boolean successful) {
        if (shuttingDown || initialized || !is_initializing) return;
        if (!successful) {
            RinkuUtil.RINKU_critical_failure = true;
            RinkuUtil.rinku_initialized = false;
            is_initializing = false;
            LOGGER.error("[FANCYMENU] Cannot initialize BrowserHandler because Rinku initialization failed!");
            return;
        }

        try {
            // These native client integrations must be installed synchronously in Rinku's init callback. Rinku creates
            // its browser preload pool immediately after the callback returns.
            if (!ActionBridge.initializeIfNecessary()) throw new IllegalStateException("Failed to initialize the Rinku action bridge");
            BrowserLoadEventListenerManager.getInstance().initialize();

            long registeredVolumeListenerId = MinecraftSoundSettingsObserver.registerVolumeListener(BrowserHandler::onVolumeUpdated);
            volumeListenerId = registeredVolumeListenerId;
            RinkuUtil.rinku_initialized = true;
            initialized = true;
            is_initializing = false;
            LOGGER.info("[FANCYMENU] BrowserHandler successfully initialized!");
        } catch (Exception ex) {
            RinkuUtil.rinku_initialized = false;
            is_initializing = false;
            LOGGER.error("[FANCYMENU] Failed to initialize BrowserHandler!", ex);
        }
    }

    public static void notifyHandler(@NotNull String identifier, @NotNull WrappedRinkuBrowser browser) {
        long now = System.currentTimeMillis();
        WrappedRinkuBrowser staleBrowser = null;
        synchronized (BROWSERS) {
            if (shuttingDown) {
                staleBrowser = browser;
            } else {
                Pair<WrappedRinkuBrowser, Long> cached = BROWSERS.get(identifier);
                if ((cached == null) || (cached.getFirst() != browser)) {
                    if ((cached != null) && (cached.getFirst() != null) && (cached.getFirst() != browser) && !cached.getFirst().isClosed()) {
                        staleBrowser = cached.getFirst();
                    }
                    BROWSERS.put(identifier, Pair.of(browser, now));
                } else {
                    cached.setSecond(now);
                }
            }
        }
        closeBrowserQuietly(staleBrowser, "stale");
    }

    @Nullable
    public static WrappedRinkuBrowser get(@NotNull String identifier) {
        if (shuttingDown) return null;
        synchronized (BROWSERS) {
            Pair<WrappedRinkuBrowser, Long> browser = BROWSERS.get(identifier);
            return (browser != null) ? browser.getFirst() : null;
        }
    }

    public static void remove(@NotNull String identifier, boolean close) {
        Pair<WrappedRinkuBrowser, Long> browser;
        synchronized (BROWSERS) {
            browser = BROWSERS.remove(identifier);
        }
        if (close && (browser != null)) closeBrowserQuietly(browser.getFirst(), "removed");
    }

    public static void tick() {
        if (shuttingDown) return;
        long now = System.currentTimeMillis();
        List<WrappedRinkuBrowser> garbageCollect = new ArrayList<>();
        synchronized (BROWSERS) {
            List<String> staleIdentifiers = new ArrayList<>();
            for (Map.Entry<String, Pair<WrappedRinkuBrowser, Long>> entry : BROWSERS.entrySet()) {
                //Close browser after 5 seconds of inactivity
                if ((entry.getValue().getSecond() + 5000) < now) {
                    staleIdentifiers.add(entry.getKey());
                    garbageCollect.add(entry.getValue().getFirst());
                }
            }
            staleIdentifiers.forEach(BROWSERS::remove);
        }
        garbageCollect.forEach(browser -> closeBrowserQuietly(browser, "inactive"));
    }

    public static void mouseMoved(double mouseX, double mouseY) {
        if (shuttingDown) return;
        getBrowserSnapshot().forEach(browser -> browser.mouseMoved(mouseX, mouseY));
    }

    public static void onVolumeUpdated(SoundSource soundSource, float newVolume) {
        if (shuttingDown) return;
        getBrowserSnapshot().forEach(browser -> browser.onVolumeUpdated(soundSource, newVolume));
    }

    public static void closeAll() {
        Long registeredVolumeListenerId;
        synchronized (BrowserHandler.class) {
            shuttingDown = true;
            initialized = false;
            is_initializing = false;
            registeredVolumeListenerId = volumeListenerId;
            volumeListenerId = null;
        }

        List<WrappedRinkuBrowser> browsers;
        synchronized (BROWSERS) {
            browsers = new ArrayList<>(BROWSERS.size());
            BROWSERS.values().forEach(browser -> browsers.add(browser.getFirst()));
            BROWSERS.clear();
        }
        browsers.forEach(browser -> closeBrowserQuietly(browser, "client shutdown"));

        if (registeredVolumeListenerId != null) {
            try {
                MinecraftSoundSettingsObserver.unregisterVolumeListener(registeredVolumeListenerId);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to unregister the Rinku browser volume listener during client shutdown!", ex);
            }
        }
    }

    @NotNull
    private static List<WrappedRinkuBrowser> getBrowserSnapshot() {
        synchronized (BROWSERS) {
            List<WrappedRinkuBrowser> browsers = new ArrayList<>(BROWSERS.size());
            BROWSERS.values().forEach(browser -> browsers.add(browser.getFirst()));
            return browsers;
        }
    }

    private static void closeBrowserQuietly(@Nullable WrappedRinkuBrowser browser, @NotNull String reason) {
        if ((browser == null) || browser.isClosed()) return;
        try {
            browser.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close {} RinkuBrowser!", reason, ex);
        }
    }

}
