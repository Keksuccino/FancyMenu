package de.keksuccino.fancymenu.mixin.support.client;

/**
 * Tracks client drowning episodes while the listener is active. Reactivation silently adopts an episode that began while dormant so the next damage packet cannot emit a synthetic start event.
 */
public final class DrowningEpisodeTracker {

    private boolean active;
    private boolean dormant;

    public DrowningEpisodeTracker() {
    }

    public void deactivate() {
        this.active = false;
        this.dormant = true;
    }

    public void prepare(boolean alreadyDrowning) {
        if (!this.dormant) return;
        this.active = alreadyDrowning;
        this.dormant = false;
    }

    public boolean needsPreparation() {
        return this.dormant;
    }

    public boolean beginEpisode() {
        if (this.active) return false;
        this.active = true;
        return true;
    }

    public void recover() {
        this.active = false;
    }

}
