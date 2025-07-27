package de.keksuccino.fancymenu.util.mcef;

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

    public static void init() {
        // Initialize the ActionBridge for JavaScript-to-Java communication
        ActionBridge.initialize();
        
        MinecraftSoundSettingsObserver.registerVolumeListener(BrowserHandler::onVolumeUpdated);
    }

    public static void notifyHandler(@NotNull String identifier, @NotNull WrappedMCEFBrowser browser) {
        if (!BROWSERS.containsKey(identifier)) {
            BROWSERS.put(identifier, Pair.of(browser, System.currentTimeMillis()));
        }
        BROWSERS.get(identifier).setValue(System.currentTimeMillis());
    }

    @Nullable
    public static WrappedMCEFBrowser get(@NotNull String identifier) {
        Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(identifier);
        if (browser != null) return browser.getKey();
        return null;
    }

    public static void remove(@NotNull String identifier, boolean close) {
        try {
            if (close) {
                Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(identifier);
                if (browser != null) browser.getKey().close();
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
            if ((m.getValue().getValue() + 5000) < now) {
                garbageCollect.add(m.getKey());
            }
        }
        garbageCollect.forEach(s -> {
            try {
                Pair<WrappedMCEFBrowser, Long> browser = BROWSERS.get(s);
                if (browser != null) browser.getKey().close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to force-close MCEFBrowser!", ex);
            }
            BROWSERS.remove(s);
        });
    }

    public static void mouseMoved(double mouseX, double mouseY) {
        BROWSERS.forEach((id, browser) -> browser.getKey().mouseMoved(mouseX, mouseY));
    }

    public static void onVolumeUpdated(SoundSource soundSource, float newVolume) {
        BROWSERS.forEach((s, wrappedMCEFBrowserLongPair) -> wrappedMCEFBrowserLongPair.getKey().onVolumeUpdated(soundSource, newVolume));
    }

}
