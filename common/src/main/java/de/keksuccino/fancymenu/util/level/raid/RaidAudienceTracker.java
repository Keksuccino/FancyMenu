package de.keksuccino.fancymenu.util.level.raid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Decides which {@link RaidAudienceEvent}s the players of one raid need, based on the audience of the raid's boss bar.
 * The audience is exactly the set of players that see the raid bar at the top of their screen.
 * <p>
 * Players only get tracked once they can receive events, so players whose FancyMenu handshake completes later still
 * get told about the raid they are in. The tracker is generic over the player type to stay independent of Minecraft's
 * runtime and relies on the player type's {@code equals} like the boss bar's own player set does.
 * <p>
 * All methods have to be called on the thread that ticks the raid.
 */
public class RaidAudienceTracker<P> {

    private final Set<P> syncedPlayers = new LinkedHashSet<>();
    private final Set<P> startingPlayers = new HashSet<>();
    private boolean awaitingFirstAudienceUpdate;
    @Nullable
    private Boolean wasOver;
    @Nullable
    private RaidSnapshot lastSentSnapshot;

    /**
     * Marks the raid as newly started, so the players of its first audience update count as being there when it started.
     * Raids loaded from a save must not call this, because they were already going on before.
     */
    public void markFreshlyCreated() {
        this.awaitingFirstAudienceUpdate = true;
        this.wasOver = false;
    }

    /**
     * Has to be called every time the raid refreshed its boss bar audience.
     *
     * @param audience The players that currently see the raid bar.
     */
    public void onAudienceUpdated(@NotNull Collection<P> audience) {
        if (!this.awaitingFirstAudienceUpdate) return;
        this.awaitingFirstAudienceUpdate = false;
        this.startingPlayers.addAll(audience);
    }

    /**
     * Has to be called at the end of every raid tick.
     *
     * @param audience The players that currently see the raid bar. Should have a fast {@code contains}.
     * @param canReceive If a player is able to receive raid events yet.
     * @param over If the raid ended in a victory or defeat.
     * @param victory If the raid ended in a victory.
     * @param snapshotSupplier Builds the current raid state. Only gets called when there is someone to send it to.
     * @param sink Receives the events in the order they need to get delivered.
     */
    public void onTick(@NotNull Collection<P> audience, @NotNull Predicate<P> canReceive, boolean over, boolean victory, @NotNull Supplier<RaidSnapshot> snapshotSupplier, @NotNull Sink<P> sink) {

        boolean finishedNow = Boolean.FALSE.equals(this.wasOver) && over;
        this.wasOver = over;

        Iterator<P> synced = this.syncedPlayers.iterator();
        while (synced.hasNext()) {
            P player = synced.next();
            if (!audience.contains(player)) {
                synced.remove();
                sink.send(player, RaidAudienceEvent.LEFT, null, false);
            }
        }
        // Players that leave before they could receive the start event only count as entering when they come back
        if (!this.startingPlayers.isEmpty()) this.startingPlayers.retainAll(audience);

        List<P> newPlayers = null;
        for (P player : audience) {
            if (!this.syncedPlayers.contains(player) && canReceive.test(player)) {
                if (newPlayers == null) newPlayers = new ArrayList<>(2);
                newPlayers.add(player);
            }
        }

        if (this.syncedPlayers.isEmpty() && (newPlayers == null)) {
            this.lastSentSnapshot = null;
            return;
        }

        RaidSnapshot snapshot = snapshotSupplier.get();
        if (finishedNow) {
            for (P player : this.syncedPlayers) {
                sink.send(player, RaidAudienceEvent.FINISHED, snapshot, victory);
            }
        } else if (!snapshot.equals(this.lastSentSnapshot)) {
            for (P player : this.syncedPlayers) {
                sink.send(player, RaidAudienceEvent.UPDATED, snapshot, false);
            }
        }
        if (newPlayers != null) {
            for (P player : newPlayers) {
                RaidAudienceEvent event = this.startingPlayers.remove(player) ? RaidAudienceEvent.STARTED : RaidAudienceEvent.ENTERED;
                this.syncedPlayers.add(player);
                sink.send(player, event, snapshot, false);
            }
        }
        this.lastSentSnapshot = snapshot;

    }

    /**
     * Has to be called when the raid got stopped and removed, which also happens after its post-raid celebration.
     */
    public void onStopped(@NotNull Sink<P> sink) {
        List<P> players = List.copyOf(this.syncedPlayers);
        this.syncedPlayers.clear();
        this.startingPlayers.clear();
        this.awaitingFirstAudienceUpdate = false;
        this.lastSentSnapshot = null;
        for (P player : players) {
            sink.send(player, RaidAudienceEvent.REMOVED, null, false);
        }
    }

    @FunctionalInterface
    public interface Sink<P> {

        /**
         * @param snapshot The current raid state for events that {@link RaidAudienceEvent#carriesSnapshot()}, otherwise NULL.
         * @param victory If the raid ended in a victory. Only meaningful for {@link RaidAudienceEvent#FINISHED}.
         */
        void send(@NotNull P player, @NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory);

    }

}
