package de.keksuccino.fancymenu.mixin.support.client;

public final class WorldSoundSubscriptionPolicy {

    private boolean registered;
    private int dispatchDepth;
    private boolean removalQueued;

    public WorldSoundSubscriptionPolicy() {
    }

    public Decision setActive(boolean active) {
        if (active) {
            if (!this.registered) {
                this.registered = true;
                return Decision.ADD;
            }
            return Decision.NONE;
        }
        if (!this.registered) return Decision.NONE;
        if (this.dispatchDepth == 0) {
            this.registered = false;
            return Decision.REMOVE;
        }
        if (!this.removalQueued) {
            this.removalQueued = true;
            return Decision.DEFER_REMOVE;
        }
        return Decision.NONE;
    }

    public void beginDispatch() {
        this.dispatchDepth++;
    }

    public void endDispatch() {
        if (this.dispatchDepth <= 0) throw new IllegalStateException("World sound dispatch depth underflow");
        this.dispatchDepth--;
    }

    public Decision reevaluateDeferredRemoval(boolean active) {
        this.removalQueued = false;
        return this.setActive(active);
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public boolean isRemovalQueued() {
        return this.removalQueued;
    }

    public enum Decision {
        NONE,
        ADD,
        REMOVE,
        DEFER_REMOVE
    }

}
