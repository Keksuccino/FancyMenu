package de.keksuccino.fancymenu.networking.packets.raid;

import de.keksuccino.fancymenu.util.level.raid.RaidAudienceEvent;
import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveRaidPacketTest {

    private static final RaidSnapshot SNAPSHOT = new RaidSnapshot(7, 3, List.of("minecraft:pillager#0d9a4b2e-5f1c-4a8e-9b3d-6c2f1e7a8b90", "minecraft:ravager#7b1c2d3e-4f50-4617-8a9b-0c1d2e3f4a5b"));

    private final ActiveRaidPacketCodec codec = new ActiveRaidPacketCodec();

    @Test
    void packetSurvivesTheNetworkCodec() {
        ActiveRaidPacket received = this.roundTrip(ActiveRaidPacket.create(42, RaidAudienceEvent.FINISHED, SNAPSHOT, true));

        assertEquals(RaidAudienceEvent.FINISHED, received.event_type);
        assertEquals(42, received.raid_id);
        assertTrue(received.victory);
        assertEquals(SNAPSHOT, received.toSnapshot());
    }

    @Test
    void everyEventTypeSurvivesTheNetworkCodec() {
        for (RaidAudienceEvent event : RaidAudienceEvent.values()) {
            RaidSnapshot snapshot = event.carriesSnapshot() ? SNAPSHOT : null;
            ActiveRaidPacket received = this.roundTrip(ActiveRaidPacket.create(1, event, snapshot, false));
            assertEquals(event, received.event_type);
            assertEquals(snapshot, received.toSnapshot());
        }
    }

    @Test
    void eventsWithoutRaidStateHaveNoSnapshot() {
        ActiveRaidPacket packet = ActiveRaidPacket.create(1, RaidAudienceEvent.LEFT, SNAPSHOT, false);
        assertNull(packet.toSnapshot());
        assertNull(ActiveRaidPacket.create(1, RaidAudienceEvent.REMOVED, null, false).toSnapshot());
    }

    @Test
    void malformedRaiderListsAreSanitized() {
        ActiveRaidPacket packet = new ActiveRaidPacket();
        packet.event_type = RaidAudienceEvent.UPDATED;
        packet.total_waves = 5;
        packet.current_wave = 2;
        RaidSnapshot withoutList = packet.toSnapshot();
        assertNotNull(withoutList);
        assertEquals(0, withoutList.aliveRaidersCount());

        packet.raiders = Arrays.asList("minecraft:witch#7b1c2d3e-4f50-4617-8a9b-0c1d2e3f4a5b", null, " ");
        assertEquals(new RaidSnapshot(5, 2, List.of("minecraft:witch#7b1c2d3e-4f50-4617-8a9b-0c1d2e3f4a5b")), packet.toSnapshot());
    }

    @Test
    void unknownEventTypesAreDroppedWithoutState() {
        ActiveRaidPacket packet = this.codec.deserialize("{\"event_type\":\"EXPLODED\",\"raid_id\":3,\"total_waves\":5}");
        assertNotNull(packet);
        assertNull(packet.event_type);
        assertNull(packet.toSnapshot());
    }

    @Test
    void serverIgnoresThePacketWhenClientsSendIt() {
        assertFalse(ActiveRaidPacket.create(1, RaidAudienceEvent.ENTERED, SNAPSHOT, false).processPacket(null));
    }

    private ActiveRaidPacket roundTrip(ActiveRaidPacket packet) {
        String serialized = Objects.requireNonNull(this.codec.serialize(packet));
        String prefix = this.codec.getPacketIdentifier() + ":";
        assertTrue(serialized.startsWith(prefix));
        return Objects.requireNonNull(this.codec.deserialize(serialized.substring(prefix.length())));
    }

}
