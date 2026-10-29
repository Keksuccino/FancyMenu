package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveRaidPlaceholdersTest {

    private static final String PILLAGER = "minecraft:pillager#0d9a4b2e-5f1c-4a8e-9b3d-6c2f1e7a8b90";
    private static final String RAVAGER = "minecraft:ravager#7b1c2d3e-4f50-4617-8a9b-0c1d2e3f4a5b";
    private static final RaidSnapshot RAID = new RaidSnapshot(8, 3, List.of(PILLAGER, RAVAGER));

    @Test
    void placeholdersReturnTheActiveRaidState() {
        assertEquals("8", new ActiveRaidTotalWavesPlaceholder().getReplacementFor(RAID));
        assertEquals("5", new ActiveRaidRemainingWavesPlaceholder().getReplacementFor(RAID));
        assertEquals("3", new ActiveRaidCurrentWavePlaceholder().getReplacementFor(RAID));
        assertEquals("2", new ActiveRaidAliveRaidersCountPlaceholder().getReplacementFor(RAID));
        assertEquals(PILLAGER + "," + RAVAGER, new ActiveRaidAliveRaidersListPlaceholder().getReplacementFor(RAID));
    }

    @Test
    void placeholdersFallBackWithoutActiveRaid() {
        assertEquals("0", new ActiveRaidTotalWavesPlaceholder().getReplacementFor((RaidSnapshot) null));
        assertEquals("0", new ActiveRaidRemainingWavesPlaceholder().getReplacementFor((RaidSnapshot) null));
        assertEquals("0", new ActiveRaidCurrentWavePlaceholder().getReplacementFor((RaidSnapshot) null));
        assertEquals("0", new ActiveRaidAliveRaidersCountPlaceholder().getReplacementFor((RaidSnapshot) null));
        assertEquals("", new ActiveRaidAliveRaidersListPlaceholder().getReplacementFor((RaidSnapshot) null));
    }

    @Test
    void placeholdersCanRunOffTheMainThread() {
        List<AbstractActiveRaidPlaceholder> placeholders = List.of(new ActiveRaidTotalWavesPlaceholder(), new ActiveRaidRemainingWavesPlaceholder(), new ActiveRaidCurrentWavePlaceholder(), new ActiveRaidAliveRaidersCountPlaceholder(), new ActiveRaidAliveRaidersListPlaceholder());
        for (AbstractActiveRaidPlaceholder placeholder : placeholders) {
            assertTrue(placeholder.canRunAsync(), placeholder.getIdentifier());
        }
    }

}
