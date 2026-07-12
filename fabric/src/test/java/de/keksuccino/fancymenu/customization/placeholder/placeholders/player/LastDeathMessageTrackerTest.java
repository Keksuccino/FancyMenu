package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LastDeathMessageTrackerTest {

    @AfterEach
    void clearTracker() {
        LastDeathMessageTracker.record(null);
    }

    @Test
    void capturesPlainTextAndJsonAtDeathTime() {
        Component deathMessage = Component.literal("Player ").append(Component.literal("fell from a high place"));

        LastDeathMessageTracker.record(deathMessage);

        assertEquals("Player fell from a high place", LastDeathMessageTracker.getPlainText());
        assertEquals("Player fell from a high place", Component.Serializer.fromJson(JsonParser.parseString(LastDeathMessageTracker.getJson())).getString());
    }

    @Test
    void replacesAndClearsTheHistoricalMessage() {
        LastDeathMessageTracker.record(Component.literal("First death"));
        LastDeathMessageTracker.record(Component.literal("Second death"));

        assertEquals("Second death", LastDeathMessageTracker.getPlainText());

        LastDeathMessageTracker.record(null);

        assertNull(LastDeathMessageTracker.getPlainText());
        assertNull(LastDeathMessageTracker.getJson());
    }
}
