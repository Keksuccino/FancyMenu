package de.keksuccino.fancymenu.util.level.raid;

import de.keksuccino.fancymenu.networking.ClientPacketUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Client-side state of the raids the server reported the player to be in. The active raid is the one whose raid bar
 * the player saw appear last, since vanilla only shows the raid bar of the closest raid (other raid bars only stay
 * around for a moment when walking from one raid into another).
 * <p>
 * The state belongs to the connection it was received on. The server does not send anything when the player
 * disconnects, so the state of an old connection is simply ignored instead of relying on disconnect callbacks.
 * <p>
 * Changes have to happen on the client main thread, which is where FancyMenu packets get processed. Reading is
 * thread-safe, because readers only ever see an immutable view.
 */
public class ActiveRaidClientState {

    public static final ActiveRaidClientState INSTANCE = new ActiveRaidClientState(ClientPacketUtils::getConnectedConnection);

    @NotNull
    private final Supplier<Object> currentConnectionSupplier;
    private final Map<Integer, RaidSnapshot> raids = new LinkedHashMap<>();
    @NotNull
    private WeakReference<Object> raidsConnection = new WeakReference<>(null);
    @NotNull
    private volatile View view = View.EMPTY;

    /**
     * @param currentConnectionSupplier Supplies the connection the client is currently connected through, or NULL.
     */
    public ActiveRaidClientState(@NotNull Supplier<Object> currentConnectionSupplier) {
        this.currentConnectionSupplier = Objects.requireNonNull(currentConnectionSupplier);
    }

    /**
     * @param connection The connection the raid event was received on.
     * @param snapshot The new raid state for events that {@link RaidAudienceEvent#carriesSnapshot()}.
     */
    public void apply(@NotNull Object connection, @NotNull RaidAudienceEvent event, int raidId, @Nullable RaidSnapshot snapshot) {
        Objects.requireNonNull(connection);
        if (this.raidsConnection.get() != connection) {
            this.raids.clear();
            this.raidsConnection = new WeakReference<>(connection);
        }
        switch (event) {
            case STARTED, ENTERED -> {
                // Re-inserting moves the raid to the end, which makes it the active one
                this.raids.remove(raidId);
                if (snapshot != null) this.raids.put(raidId, snapshot);
            }
            case UPDATED, FINISHED -> {
                if (snapshot != null) this.raids.put(raidId, snapshot);
            }
            case LEFT, REMOVED -> this.raids.remove(raidId);
        }
        RaidSnapshot activeRaid = null;
        for (RaidSnapshot raid : this.raids.values()) {
            activeRaid = raid;
        }
        this.view = (activeRaid != null) ? new View(this.raidsConnection, activeRaid) : View.EMPTY;
    }

    /**
     * @return The raid whose raid bar the player currently sees, or NULL if there is none.
     */
    @Nullable
    public RaidSnapshot getActiveRaid() {
        View currentView = this.view;
        if (currentView.activeRaid() == null) return null;
        Object connection = this.currentConnectionSupplier.get();
        if ((connection == null) || (connection != currentView.connection().get())) return null;
        return currentView.activeRaid();
    }

    public boolean isInActiveRaidArea() {
        return this.getActiveRaid() != null;
    }

    private record View(@NotNull WeakReference<Object> connection, @Nullable RaidSnapshot activeRaid) {

        private static final View EMPTY = new View(new WeakReference<>(null), null);

    }

}
