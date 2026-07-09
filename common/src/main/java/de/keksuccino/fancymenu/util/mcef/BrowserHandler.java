package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
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

        if (!MCEF.isInitialized()) {
            LOGGER.warn("[FANCYMENU] MCEF not initialized yet! Will wait for MCEF to be ready before initializing BrowserHandler!");
        }

        FancyMenuThreads.startDaemonThread(() -> {
            try {
                while (!shuttingDown) {
                    if (MCEFUtil.MCEF_initialized || MCEF.isInitialized()) {
                        MCEFUtil.MCEF_initialized = true;
                        MainThreadTaskExecutor.executeInMainThread(() -> {
                            if (shuttingDown) return;
                            try {

                                // Initialize the ActionBridge for JavaScript-to-Java communication
                                ActionBridge.initialize();

                                long registeredVolumeListenerId = MinecraftSoundSettingsObserver.registerVolumeListener(BrowserHandler::onVolumeUpdated);
                                synchronized (BrowserHandler.class) {
                                    if (shuttingDown) {
                                        MinecraftSoundSettingsObserver.unregisterVolumeListener(registeredVolumeListenerId);
                                        is_initializing = false;
                                        return;
                                    }
                                    volumeListenerId = registeredVolumeListenerId;
                                    initialized = true;
                                    is_initializing = false;
                                }

                                LOGGER.info("[FANCYMENU] BrowserHandler successfully initialized!");

                            } catch (Exception ex) {
                                is_initializing = false;
                                LOGGER.error("[FANCYMENU] Failed to initialize BrowserHandler!", ex);
                            }
                        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
                is_initializing = false;
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                is_initializing = false;
                if (!shuttingDown) LOGGER.error("[FANCYMENU] Failed to initialize BrowserHandler!", ex);
            }
        }, "BrowserHandler-Initialization");

    }

    public static void notifyHandler(@NotNull String identifier, @NotNull WrappedMCEFBrowser browser) {
        long now = System.currentTimeMillis();
        WrappedMCEFBrowser staleBrowser = null;
        synchronized (BROWSERS) {
            if (shuttingDown) {
                staleBrowser = browser;
            } else {
                Pair<WrappedMCEFBrowser, Long> cached = BROWSERS.get(identifier);
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
    public static WrappedMCEFBrowser get(@NotNull String identifier) {
        if (shuttingDown) return null;
        synchronized (BROWSERS) {
            Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(identifier);
            return (browser != null) ? browser.getFirst() : null;
        }
    }

    public static void remove(@NotNull String identifier, boolean close) {
        Pair<WrappedMCEFBrowser, Long> browser;
        synchronized (BROWSERS) {
            browser = BROWSERS.remove(identifier);
        }
        if (close && (browser != null)) closeBrowserQuietly(browser.getFirst(), "removed");
    }

    public static void tick() {
        if (shuttingDown) return;
        long now = System.currentTimeMillis();
        List<WrappedMCEFBrowser> garbageCollect = new ArrayList<>();
        synchronized (BROWSERS) {
            List<String> staleIdentifiers = new ArrayList<>();
            for (Map.Entry<String, Pair<WrappedMCEFBrowser, Long>> entry : BROWSERS.entrySet()) {
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

        List<WrappedMCEFBrowser> browsers;
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
                LOGGER.error("[FANCYMENU] Failed to unregister the MCEF browser volume listener during client shutdown!", ex);
            }
        }
    }

    @NotNull
    private static List<WrappedMCEFBrowser> getBrowserSnapshot() {
        synchronized (BROWSERS) {
            List<WrappedMCEFBrowser> browsers = new ArrayList<>(BROWSERS.size());
            BROWSERS.values().forEach(browser -> browsers.add(browser.getFirst()));
            return browsers;
        }
    }

    private static void closeBrowserQuietly(@Nullable WrappedMCEFBrowser browser, @NotNull String reason) {
        if ((browser == null) || browser.isClosed()) return;
        try {
            browser.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close {} MCEFBrowser!", reason, ex);
        }
    }

}
