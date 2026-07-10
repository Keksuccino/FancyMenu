package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastDeathMessageTrackerTest {

    @AfterEach
    void clearTracker() {
        LastDeathMessageTracker.record(null);
    }

    @Test
    void recordsPlainTextAndJsonWithoutMinecraftRuntimeState() {
        LastDeathMessageTracker.record(Component.literal("Fell from a high place"));

        assertEquals("Fell from a high place", LastDeathMessageTracker.getPlainText());
        assertTrue(LastDeathMessageTracker.getJson().contains("Fell from a high place"));
    }

    @Test
    void capturesHistoricalValueAtRecordTime() {
        MutableComponent message = Component.literal("Original message");
        LastDeathMessageTracker.record(message);

        message.append(" changed later");

        assertEquals("Original message", LastDeathMessageTracker.getPlainText());
        assertEquals(Component.Serializer.toJson(Component.literal("Original message")), LastDeathMessageTracker.getJson());
    }

    @Test
    void nullMessageClearsBothCachedForms() {
        LastDeathMessageTracker.record(Component.literal("Temporary message"));

        LastDeathMessageTracker.record(null);

        assertNull(LastDeathMessageTracker.getPlainText());
        assertNull(LastDeathMessageTracker.getJson());
    }
}
