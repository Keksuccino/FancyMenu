package de.keksuccino.fancymenu.util.level.raid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RaidSnapshotTest {

    private static final UUID PILLAGER_UUID = UUID.fromString("0d9a4b2e-5f1c-4a8e-9b3d-6c2f1e7a8b90");
    private static final UUID RAVAGER_UUID = UUID.fromString("7b1c2d3e-4f50-4617-8a9b-0c1d2e3f4a5b");

    @Test
    void raiderEntriesCombineEntityTypeAndUuid() {
        assertEquals("minecraft:pillager#" + PILLAGER_UUID, RaidSnapshot.formatRaiderEntry("minecraft:pillager", PILLAGER_UUID));
    }

    @Test
    void raidersGetJoinedWithCommasWithoutSpaces() {
        String pillager = RaidSnapshot.formatRaiderEntry("minecraft:pillager", PILLAGER_UUID);
        String ravager = RaidSnapshot.formatRaiderEntry("minecraft:ravager", RAVAGER_UUID);
        RaidSnapshot snapshot = new RaidSnapshot(5, 2, List.of(pillager, ravager));

        assertEquals(pillager + "," + ravager, snapshot.joinRaiders());
        assertEquals(2, snapshot.aliveRaidersCount());
        assertEquals("", new RaidSnapshot(5, 2, List.of()).joinRaiders());
    }

    @Test
    void remainingWavesCountTheWavesThatDidNotSpawnYet() {
        assertEquals(5, new RaidSnapshot(5, 0, List.of()).remainingWaves());
        assertEquals(2, new RaidSnapshot(5, 3, List.of()).remainingWaves());
        assertEquals(0, new RaidSnapshot(5, 5, List.of()).remainingWaves());
    }

    @Test
    void invalidNumbersAreClamped() {
        RaidSnapshot snapshot = new RaidSnapshot(-1, -3, List.of());
        assertEquals(0, snapshot.totalWaves());
        assertEquals(0, snapshot.currentWave());
        assertEquals(0, new RaidSnapshot(3, 4, List.of()).remainingWaves());
    }

    @Test
    void snapshotsAreImmutableValues() {
        List<String> raiders = new ArrayList<>(List.of("minecraft:pillager#" + PILLAGER_UUID));
        RaidSnapshot snapshot = new RaidSnapshot(5, 1, raiders);
        raiders.clear();

        assertEquals(1, snapshot.aliveRaidersCount());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.raiders().clear());
        assertEquals(new RaidSnapshot(5, 1, List.of("minecraft:pillager#" + PILLAGER_UUID)), snapshot);
        assertEquals(new RaidSnapshot(5, 1, List.of("minecraft:pillager#" + PILLAGER_UUID)).hashCode(), snapshot.hashCode());
        assertNotEquals(new RaidSnapshot(5, 2, snapshot.raiders()), snapshot);
        assertNotEquals(new RaidSnapshot(7, 1, snapshot.raiders()), snapshot);
        assertNotEquals(new RaidSnapshot(5, 1, List.of()), snapshot);
    }

}
