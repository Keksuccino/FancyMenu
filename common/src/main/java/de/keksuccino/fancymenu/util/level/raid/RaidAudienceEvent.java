package de.keksuccino.fancymenu.util.level.raid;

/**
 * Changes of a player's view on a raid. A player is part of a raid's audience while its raid bar is shown to them.
 */
public enum RaidAudienceEvent {

    /** The raid started while the player was in its area. */
    STARTED(true),
    /** The player entered the area of a raid that was already going on. */
    ENTERED(true),
    /** The player left the area of a raid that is still going on. */
    LEFT(false),
    /** The raid ended in a victory or defeat while the player was in its area. */
    FINISHED(true),
    /** The state of the raid changed while the player stayed in its area. */
    UPDATED(true),
    /** The raid got removed while the player was in its area, so there is no area left to leave. */
    REMOVED(false);

    private final boolean carriesSnapshot;

    RaidAudienceEvent(boolean carriesSnapshot) {
        this.carriesSnapshot = carriesSnapshot;
    }

    public boolean carriesSnapshot() {
        return this.carriesSnapshot;
    }

}
