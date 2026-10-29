package de.keksuccino.fancymenu.util.level.raid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveRaidClientStateTest {

    private static final RaidSnapshot FIRST_RAID = new RaidSnapshot(5, 1, List.of("minecraft:pillager#00000000-0000-0000-0000-000000000001"));
    private static final RaidSnapshot FIRST_RAID_UPDATED = new RaidSnapshot(5, 2, List.of());
    private static final RaidSnapshot SECOND_RAID = new RaidSnapshot(3, 1, List.of());

    private final Object connection = new Object();
    private final AtomicReference<Object> currentConnection = new AtomicReference<>(this.connection);
    private final ActiveRaidClientState state = new ActiveRaidClientState(this.currentConnection::get);

    @Test
    void thereIsNoActiveRaidByDefault() {
        assertNull(this.state.getActiveRaid());
        assertFalse(this.state.isInActiveRaidArea());
    }

    @Test
    void startedAndEnteredRaidsBecomeActive() {
        this.state.apply(this.connection, RaidAudienceEvent.STARTED, 1, FIRST_RAID);
        assertSame(FIRST_RAID, this.state.getActiveRaid());

        this.state.apply(this.connection, RaidAudienceEvent.LEFT, 1, null);
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        assertSame(FIRST_RAID, this.state.getActiveRaid());
        assertTrue(this.state.isInActiveRaidArea());
    }

    @Test
    void updatesAndFinishesReplaceTheRaidState() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);

        this.state.apply(this.connection, RaidAudienceEvent.UPDATED, 1, FIRST_RAID_UPDATED);
        assertSame(FIRST_RAID_UPDATED, this.state.getActiveRaid());

        this.state.apply(this.connection, RaidAudienceEvent.FINISHED, 1, FIRST_RAID);
        assertSame(FIRST_RAID, this.state.getActiveRaid());
    }

    @Test
    void leavingOrRemovingTheRaidEndsTheActiveRaid() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        this.state.apply(this.connection, RaidAudienceEvent.LEFT, 1, null);
        assertNull(this.state.getActiveRaid());

        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        this.state.apply(this.connection, RaidAudienceEvent.REMOVED, 1, null);
        assertNull(this.state.getActiveRaid());
    }

    @Test
    void lastEnteredRaidStaysActiveWhileAnOlderRaidGetsUpdated() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 2, SECOND_RAID);

        this.state.apply(this.connection, RaidAudienceEvent.UPDATED, 1, FIRST_RAID_UPDATED);
        assertSame(SECOND_RAID, this.state.getActiveRaid());

        this.state.apply(this.connection, RaidAudienceEvent.LEFT, 2, null);
        assertSame(FIRST_RAID_UPDATED, this.state.getActiveRaid());
    }

    @Test
    void leavingAnOlderRaidKeepsTheNewerOneActive() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 2, SECOND_RAID);

        this.state.apply(this.connection, RaidAudienceEvent.LEFT, 1, null);

        assertSame(SECOND_RAID, this.state.getActiveRaid());
    }

    @Test
    void stateOfAnOldConnectionIsIgnored() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);

        this.currentConnection.set(new Object());
        assertNull(this.state.getActiveRaid());

        this.currentConnection.set(null);
        assertNull(this.state.getActiveRaid());
    }

    @Test
    void eventsOfANewConnectionDropTheRaidsOfTheOldOne() {
        this.state.apply(this.connection, RaidAudienceEvent.ENTERED, 1, FIRST_RAID);
        Object newConnection = new Object();
        this.currentConnection.set(newConnection);

        this.state.apply(newConnection, RaidAudienceEvent.ENTERED, 2, SECOND_RAID);
        this.state.apply(newConnection, RaidAudienceEvent.LEFT, 2, null);

        assertNull(this.state.getActiveRaid());
    }

}
