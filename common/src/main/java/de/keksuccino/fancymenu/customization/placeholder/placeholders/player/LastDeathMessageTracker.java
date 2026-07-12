package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the last local-player death message independently of the optional player-death listener.
 * Both forms are captured at death time so later language changes cannot alter the placeholder's historical value.
 */
public final class LastDeathMessageTracker {

    private static final Snapshot EMPTY_SNAPSHOT = new Snapshot(null, null);

    private static volatile Snapshot snapshot = EMPTY_SNAPSHOT;

    private LastDeathMessageTracker() {
    }

    public static void record(@Nullable Component deathMessage) {
        snapshot = deathMessage != null ? new Snapshot(deathMessage.getString(), ComponentParser.toJson(deathMessage)) : EMPTY_SNAPSHOT;
    }

    @Nullable
    public static String getPlainText() {
        return snapshot.plainText;
    }

    @Nullable
    public static String getJson() {
        return snapshot.json;
    }

    /**
     * Publishing both representations as one immutable snapshot prevents readers from observing values from different deaths.
     */
    private record Snapshot(@Nullable String plainText, @Nullable String json) {
    }
}
