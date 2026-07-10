package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the last local-player death message independently of the optional player-death listener.
 * Both forms are captured at death time so later language changes cannot alter the placeholder's historical value.
 */
public final class LastDeathMessageTracker {

    @Nullable private static String cachedPlainText;
    @Nullable private static String cachedJson;

    private LastDeathMessageTracker() {
    }

    public static synchronized void record(@Nullable Component deathMessage) {
        if (deathMessage == null) {
            cachedPlainText = null;
            cachedJson = null;
            return;
        }
        cachedPlainText = deathMessage.getString();
        cachedJson = ComponentParser.toJson(deathMessage);
    }

    @Nullable
    public static synchronized String getPlainText() {
        return cachedPlainText;
    }

    @Nullable
    public static synchronized String getJson() {
        return cachedJson;
    }
}
