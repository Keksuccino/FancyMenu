package de.keksuccino.fancymenu.mixin.mixins.common.client;

/**
 * Tracks client drowning episodes while the listener is active. Reactivation silently adopts an episode that began while dormant so the next damage packet cannot emit a synthetic start event.
 */
final class DrowningEpisodeTracker {

    private boolean active;
    private boolean dormant;

    void deactivate() {
        this.active = false;
        this.dormant = true;
    }

    void prepare(boolean alreadyDrowning) {
        if (!this.dormant) return;
        this.active = alreadyDrowning;
        this.dormant = false;
    }

    boolean needsPreparation() {
        return this.dormant;
    }

    boolean beginEpisode() {
        if (this.active) return false;
        this.active = true;
        return true;
    }

    void recover() {
        this.active = false;
    }

}
