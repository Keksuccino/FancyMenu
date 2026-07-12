package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
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
    private static final HashMap<String, Pair<WrappedMCEFBrowser, Long>> BROWSERS = new HashMap<>();

    private static boolean is_initializing = false;
    public static boolean initialized = false;

    public static synchronized void init() {
        if (initialized || is_initializing) return;
        is_initializing = true;

        LOGGER.info("[FANCYMENU] Starting initialization of BrowserHandler..");

        if (MCEF.isInitialized()) {
            completeInitialization(true);
            return;
        }

        LOGGER.warn("[FANCYMENU] MCEF not initialized yet! Registering FancyMenu's browser integration for MCEF's initialization phase.");

        // MCEF invokes these callbacks after creating its client but before creating its preloaded browsers. The
        // message router must exist before those browser contexts or the first pooled browser never receives cefQuery.
        MCEF.scheduleForInit(BrowserHandler::completeInitialization);

        // MCEF and both loader startup paths initialize on the client thread. Keep an idempotent recheck as a defensive
        // fallback for integrations that report MCEF ready immediately after listener registration.
        if (MCEF.isInitialized()) completeInitialization(true);
    }

    private static synchronized void completeInitialization(boolean successful) {
        if (initialized || !is_initializing) return;
        if (!successful) {
            MCEFUtil.MCEF_critical_failure = true;
            MCEFUtil.MCEF_initialized = false;
            is_initializing = false;
            LOGGER.error("[FANCYMENU] Cannot initialize BrowserHandler because MCEF initialization failed!");
            return;
        }

        try {
            // These native client integrations must be installed synchronously in MCEF's init callback. MCEF creates
            // its browser preload pool immediately after the callback returns.
            if (!ActionBridge.initializeIfNecessary()) throw new IllegalStateException("Failed to initialize the MCEF action bridge");
            BrowserLoadEventListenerManager.getInstance().initialize();

            MinecraftSoundSettingsObserver.registerVolumeListener(BrowserHandler::onVolumeUpdated);
            MCEFUtil.MCEF_initialized = true;
            initialized = true;
            is_initializing = false;
            LOGGER.info("[FANCYMENU] BrowserHandler successfully initialized!");
        } catch (Exception ex) {
            MCEFUtil.MCEF_initialized = false;
            is_initializing = false;
            LOGGER.error("[FANCYMENU] Failed to initialize BrowserHandler!", ex);
        }
    }

    public static void notifyHandler(@NotNull String identifier, @NotNull WrappedMCEFBrowser browser) {
        long now = System.currentTimeMillis();
        Pair<WrappedMCEFBrowser, Long> cached = BROWSERS.get(identifier);
        if ((cached == null) || (cached.getFirst() != browser)) {
            if ((cached != null) && (cached.getFirst() != null) && (cached.getFirst() != browser) && !cached.getFirst().isClosed()) {
                try {
                    cached.getFirst().close();
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to close stale MCEFBrowser!", ex);
                }
            }
            BROWSERS.put(identifier, Pair.of(browser, now));
            return;
        }
        cached.setSecond(now);
    }

    @Nullable
    public static WrappedMCEFBrowser get(@NotNull String identifier) {
        Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(identifier);
        if (browser != null) return browser.getFirst();
        return null;
    }

    public static void remove(@NotNull String identifier, boolean close) {
        try {
            if (close) {
                Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(identifier);
                if (browser != null) browser.getFirst().close();
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to force-close MCEFBrowser!", ex);
        }
        BROWSERS.remove(identifier);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        List<String> garbageCollect = new ArrayList<>();
        for (Map.Entry<String, Pair<WrappedMCEFBrowser, Long>> m : BROWSERS.entrySet()) {
            //Close browser after 5 seconds of inactivity
            if ((m.getValue().getSecond() + 5000) < now) {
                garbageCollect.add(m.getKey());
            }
        }
        garbageCollect.forEach(s -> {
            try {
                Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(s);
                if (browser != null) browser.getFirst().close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to force-close MCEFBrowser!", ex);
            }
            BROWSERS.remove(s);
        });
    }

    public static void mouseMoved(double mouseX, double mouseY) {
        BROWSERS.forEach((id, browser) -> browser.getFirst().mouseMoved(mouseX, mouseY));
    }

    public static void onVolumeUpdated(SoundSource soundSource, float newVolume) {
        BROWSERS.forEach((s, wrappedMCEFBrowserLongPair) -> wrappedMCEFBrowserLongPair.getFirst().onVolumeUpdated(soundSource, newVolume));
    }

}
