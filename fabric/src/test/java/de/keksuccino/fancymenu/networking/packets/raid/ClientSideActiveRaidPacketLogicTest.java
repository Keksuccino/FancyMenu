package de.keksuccino.fancymenu.networking.packets.raid;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.level.raid.ActiveRaidClientState;
import de.keksuccino.fancymenu.util.level.raid.RaidAudienceEvent;
import de.keksuccino.fancymenu.util.level.raid.RaidSnapshot;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSideActiveRaidPacketLogicTest {

    private static final RaidSnapshot SNAPSHOT = new RaidSnapshot(5, 1, List.of("minecraft:pillager#0d9a4b2e-5f1c-4a8e-9b3d-6c2f1e7a8b90"));

    private final Object connection = new Object();
    private final ActiveRaidClientState state = new ActiveRaidClientState(() -> this.connection);
    private final List<Fired> fired = new ArrayList<>();
    private final List<Runnable> unregistrations = new ArrayList<>();
    private ListenerInstance finishedInstance;

    @BeforeEach
    void registerRaidListeners() {
        this.listen(Listeners.ON_RAID_STARTED, RaidAudienceEvent.STARTED);
        this.listen(Listeners.ON_ENTER_ACTIVE_RAID_AREA, RaidAudienceEvent.ENTERED);
        this.listen(Listeners.ON_LEAVE_ACTIVE_RAID_AREA, RaidAudienceEvent.LEFT);
        this.finishedInstance = this.listen(Listeners.ON_RAID_FINISHED, RaidAudienceEvent.FINISHED);
    }

    @AfterEach
    void unregisterRaidListeners() {
        this.unregistrations.forEach(Runnable::run);
    }

    @Test
    void eachEventTriggersOnlyItsListenerAfterTheStateGotUpdated() {
        this.handle(RaidAudienceEvent.STARTED, SNAPSHOT, false);
        this.handle(RaidAudienceEvent.LEFT, null, false);
        this.handle(RaidAudienceEvent.ENTERED, SNAPSHOT, false);
        this.handle(RaidAudienceEvent.FINISHED, SNAPSHOT, true);

        assertEquals(List.of(new Fired(RaidAudienceEvent.STARTED, SNAPSHOT), new Fired(RaidAudienceEvent.LEFT, null), new Fired(RaidAudienceEvent.ENTERED, SNAPSHOT), new Fired(RaidAudienceEvent.FINISHED, SNAPSHOT)), this.fired);
    }

    @Test
    void updatesAndRemovalsOnlyChangeTheState() {
        this.handle(RaidAudienceEvent.UPDATED, SNAPSHOT, false);
        assertEquals(SNAPSHOT, this.state.getActiveRaid());

        this.handle(RaidAudienceEvent.REMOVED, null, false);
        assertNull(this.state.getActiveRaid());
        assertTrue(this.fired.isEmpty());
    }

    @Test
    void finishedListenerReportsTheRaidResult() {
        this.handle(RaidAudienceEvent.FINISHED, SNAPSHOT, true);
        assertEquals("victory", this.raidResultOf(this.finishedInstance));

        this.handle(RaidAudienceEvent.FINISHED, SNAPSHOT, false);
        assertEquals("defeat", this.raidResultOf(this.finishedInstance));
    }

    @Test
    void packetsWithoutEventTypeAreRejected() {
        ActiveRaidPacket packet = ActiveRaidPacket.create(1, RaidAudienceEvent.ENTERED, SNAPSHOT, false);
        packet.event_type = null;

        assertFalse(ClientSideActiveRaidPacketLogic.handle(packet, this.connection, this.state));
        assertNull(this.state.getActiveRaid());
        assertTrue(this.fired.isEmpty());
    }

    private void handle(@NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory) {
        assertTrue(ClientSideActiveRaidPacketLogic.handle(ActiveRaidPacket.create(1, event, snapshot, victory), this.connection, this.state));
    }

    @NotNull
    private ListenerInstance listen(@NotNull AbstractListener listener, @NotNull RaidAudienceEvent event) {
        ListenerInstance instance = listener.createFreshInstance();
        instance.getActionScript().addExecutable(new RunnableExecutable(() -> this.fired.add(new Fired(event, this.state.getActiveRaid()))));
        instance.registerSelfToParent();
        this.unregistrations.add(() -> listener.unregisterInstance(instance));
        return instance;
    }

    @NotNull
    private String raidResultOf(@NotNull ListenerInstance instance) {
        return instance.getActionScript().getValuePlaceholders().get("raid_result").get();
    }

    private record Fired(@NotNull RaidAudienceEvent event, @Nullable RaidSnapshot activeRaid) {
    }

    private static final class RunnableExecutable implements Executable {

        private final String identifier = UUID.randomUUID().toString();
        private final Runnable runnable;

        private RunnableExecutable(@NotNull Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void execute() {
            this.runnable.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new RunnableExecutable(this.runnable);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test_executable");
        }

    }

}
