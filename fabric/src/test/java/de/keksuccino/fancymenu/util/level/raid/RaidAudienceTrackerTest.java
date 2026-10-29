package de.keksuccino.fancymenu.util.level.raid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaidAudienceTrackerTest {

    private static final Predicate<String> EVERYONE = player -> true;
    private static final Predicate<String> NOBODY = player -> false;
    private static final RaidSnapshot FIRST_WAVE = new RaidSnapshot(5, 1, List.of("minecraft:pillager#00000000-0000-0000-0000-000000000001"));
    private static final RaidSnapshot FIRST_WAVE_ONE_KILLED = new RaidSnapshot(5, 1, List.of());

    private final RecordingSink sink = new RecordingSink();

    @Test
    void playersInTheFirstAudienceOfNewRaidSeeItStart() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        tracker.onAudienceUpdated(audience("alex"));

        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.STARTED, FIRST_WAVE, false)), this.sink.sent);
    }

    @Test
    void playersJoiningAfterTheFirstAudienceUpdateEnterTheRaid() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        tracker.onAudienceUpdated(audience("alex"));
        tracker.onAudienceUpdated(audience("alex", "steve"));

        this.tick(tracker, audience("alex", "steve"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.STARTED), this.sink.eventsOf("alex"));
        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("steve"));
    }

    @Test
    void raidsLoadedFromSaveNeverReportAStart() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.onAudienceUpdated(audience("alex"));

        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("alex"));
    }

    @Test
    void startingPlayerWithLateHandshakeStillSeesTheStart() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        tracker.onAudienceUpdated(audience("alex"));

        this.tick(tracker, audience("alex"), NOBODY, false, false, FIRST_WAVE);
        assertTrue(this.sink.sent.isEmpty());

        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);
        assertEquals(List.of(RaidAudienceEvent.STARTED), this.sink.eventsOf("alex"));
    }

    @Test
    void startingPlayerThatLeavesBeforeReceivingTheStartEntersWhenComingBack() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        tracker.onAudienceUpdated(audience("alex"));
        this.tick(tracker, audience("alex"), NOBODY, false, false, FIRST_WAVE);
        this.tick(tracker, audience(), NOBODY, false, false, FIRST_WAVE);

        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("alex"));
    }

    @Test
    void leavingTheAudienceSendsLeftWithoutSnapshotAndReenteringSendsEntered() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");

        this.tick(tracker, audience(), EVERYONE, false, false, FIRST_WAVE);
        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.LEFT, null, false), new Sent("alex", RaidAudienceEvent.ENTERED, FIRST_WAVE, false)), this.sink.sent);
    }

    @Test
    void leftIsSentBeforeOtherPlayersEnterInTheSameTick() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");

        this.tick(tracker, audience("steve"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.LEFT, null, false), new Sent("steve", RaidAudienceEvent.ENTERED, FIRST_WAVE, false)), this.sink.sent);
    }

    @Test
    void updatesAreOnlySentWhenTheRaidStateChanges() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex", "steve");

        this.tick(tracker, audience("alex", "steve"), EVERYONE, false, false, new RaidSnapshot(5, 1, FIRST_WAVE.raiders()));
        assertTrue(this.sink.sent.isEmpty());

        this.tick(tracker, audience("alex", "steve"), EVERYONE, false, false, FIRST_WAVE_ONE_KILLED);
        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.UPDATED, FIRST_WAVE_ONE_KILLED, false), new Sent("steve", RaidAudienceEvent.UPDATED, FIRST_WAVE_ONE_KILLED, false)), this.sink.sent);
    }

    @Test
    void newPlayersOnlyGetTheirEntryEventWhileOthersGetTheUpdate() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");

        this.tick(tracker, audience("alex", "steve"), EVERYONE, false, false, FIRST_WAVE_ONE_KILLED);

        assertEquals(List.of(RaidAudienceEvent.UPDATED), this.sink.eventsOf("alex"));
        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("steve"));
    }

    @Test
    void victoryIsReportedOnceToEveryoneInTheArea() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex", "steve");
        RaidSnapshot won = new RaidSnapshot(5, 5, List.of());

        this.tick(tracker, audience("alex", "steve"), EVERYONE, true, true, won);
        this.tick(tracker, audience("alex", "steve"), EVERYONE, true, true, won);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.FINISHED, won, true), new Sent("steve", RaidAudienceEvent.FINISHED, won, true)), this.sink.sent);
    }

    @Test
    void defeatIsReportedAsNoVictory() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");

        this.tick(tracker, audience("alex"), EVERYONE, true, false, FIRST_WAVE);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.FINISHED, FIRST_WAVE, false)), this.sink.sent);
    }

    @Test
    void playersEnteringAfterTheRaidFinishedOnlyEnter() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");
        this.tick(tracker, audience("alex"), EVERYONE, true, true, FIRST_WAVE);
        this.sink.sent.clear();

        this.tick(tracker, audience("alex", "steve"), EVERYONE, true, true, FIRST_WAVE);

        assertEquals(List.of(new Sent("steve", RaidAudienceEvent.ENTERED, FIRST_WAVE, false)), this.sink.sent);
    }

    @Test
    void loadedRaidThatIsAlreadyOverDoesNotReportAFinish() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();

        this.tick(tracker, audience("alex"), EVERYONE, true, true, FIRST_WAVE);
        this.tick(tracker, audience("alex"), EVERYONE, true, true, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("alex"));
    }

    @Test
    void loadedRaidThatFinishesLaterReportsTheFinish() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        this.tick(tracker, audience("alex"), EVERYONE, false, false, FIRST_WAVE);

        this.tick(tracker, audience("alex"), EVERYONE, true, false, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.ENTERED, RaidAudienceEvent.FINISHED), this.sink.eventsOf("alex"));
    }

    @Test
    void finishWithoutAnyReceiverIsNotReportedToPlayersArrivingLater() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        this.tick(tracker, audience(), EVERYONE, true, true, FIRST_WAVE);

        this.tick(tracker, audience("alex"), EVERYONE, true, true, FIRST_WAVE);

        assertEquals(List.of(RaidAudienceEvent.ENTERED), this.sink.eventsOf("alex"));
    }

    @Test
    void stoppedRaidIsRemovedForEveryoneWithoutLeaveEvents() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex", "steve");

        tracker.onStopped(this.sink);
        this.tick(tracker, audience(), EVERYONE, true, true, FIRST_WAVE);

        assertEquals(List.of(new Sent("alex", RaidAudienceEvent.REMOVED, null, false), new Sent("steve", RaidAudienceEvent.REMOVED, null, false)), this.sink.sent);
    }

    @Test
    void stoppingRaidWithoutReceiversSendsNothing() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        tracker.markFreshlyCreated();
        tracker.onAudienceUpdated(audience("alex"));

        tracker.onStopped(this.sink);

        assertTrue(this.sink.sent.isEmpty());
    }

    @Test
    void snapshotIsOnlyBuiltWhenSomeoneCanReceiveIt() {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        AtomicInteger builds = new AtomicInteger();

        tracker.onTick(audience(), EVERYONE, false, false, () -> { builds.incrementAndGet(); return FIRST_WAVE; }, this.sink);
        tracker.onTick(audience("alex"), NOBODY, false, false, () -> { builds.incrementAndGet(); return FIRST_WAVE; }, this.sink);
        assertEquals(0, builds.get());

        tracker.onTick(audience("alex"), EVERYONE, false, false, () -> { builds.incrementAndGet(); return FIRST_WAVE; }, this.sink);
        assertEquals(1, builds.get());
    }

    @Test
    void lastPlayerLeavingResetsTheSentStateForTheNextOne() {
        RaidAudienceTracker<String> tracker = this.trackerWithSyncedPlayers("alex");
        this.tick(tracker, audience(), EVERYONE, false, false, FIRST_WAVE);
        this.sink.sent.clear();

        this.tick(tracker, audience("steve"), EVERYONE, false, false, FIRST_WAVE);

        assertEquals(List.of(new Sent("steve", RaidAudienceEvent.ENTERED, FIRST_WAVE, false)), this.sink.sent);
    }

    @NotNull
    private RaidAudienceTracker<String> trackerWithSyncedPlayers(@NotNull String... players) {
        RaidAudienceTracker<String> tracker = new RaidAudienceTracker<>();
        this.tick(tracker, audience(players), EVERYONE, false, false, FIRST_WAVE);
        assertEquals(players.length, this.sink.sent.size());
        this.sink.sent.clear();
        return tracker;
    }

    // Keeps the given order, so assertions about the event order across players are deterministic
    @NotNull
    private static Set<String> audience(@NotNull String... players) {
        return new LinkedHashSet<>(List.of(players));
    }

    private void tick(@NotNull RaidAudienceTracker<String> tracker, @NotNull Collection<String> audience, @NotNull Predicate<String> canReceive, boolean over, boolean victory, @NotNull RaidSnapshot snapshot) {
        tracker.onTick(audience, canReceive, over, victory, () -> snapshot, this.sink);
    }

    private record Sent(@NotNull String player, @NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory) {
    }

    private static final class RecordingSink implements RaidAudienceTracker.Sink<String> {

        private final List<Sent> sent = new ArrayList<>();

        @Override
        public void send(@NotNull String player, @NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory) {
            if (!event.carriesSnapshot()) assertNull(snapshot);
            this.sent.add(new Sent(player, event, snapshot, victory));
        }

        @NotNull
        private List<RaidAudienceEvent> eventsOf(@NotNull String player) {
            return this.sent.stream().filter(sent -> sent.player().equals(player)).map(Sent::event).toList();
        }

    }

}
