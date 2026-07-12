package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LastDeathMessageTrackerTest {

    @AfterEach
    void clearTracker() {
        LastDeathMessageTracker.record(null, component -> "unused");
    }

    @Test
    void capturesPlainAndSerializedFormsAtRecordTime() {
        Component message = Component.literal("Player died");

        LastDeathMessageTracker.record(message, component -> "{\"text\":\"snapshot\"}");

        assertEquals("Player died", LastDeathMessageTracker.getPlainText());
        assertEquals("{\"text\":\"snapshot\"}", LastDeathMessageTracker.getJson());
    }

    @Test
    void nullClearsBothFormsWithoutCallingSerializer() {
        LastDeathMessageTracker.record(Component.literal("old"), component -> "old-json");
        AtomicBoolean serializerCalled = new AtomicBoolean();

        LastDeathMessageTracker.record(null, component -> {
            serializerCalled.set(true);
            return "unexpected";
        });

        assertEquals(false, serializerCalled.get());
        assertNull(LastDeathMessageTracker.getPlainText());
        assertNull(LastDeathMessageTracker.getJson());
    }
}
