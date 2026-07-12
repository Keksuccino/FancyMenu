package de.keksuccino.fancymenu.mixin.mixins.common.client;

final class WorldSoundSubscriptionPolicy {

    private boolean registered;
    private int dispatchDepth;
    private boolean removalQueued;

    Decision setActive(boolean active) {
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

    void beginDispatch() {
        this.dispatchDepth++;
    }

    void endDispatch() {
        if (this.dispatchDepth <= 0) throw new IllegalStateException("World sound dispatch depth underflow");
        this.dispatchDepth--;
    }

    Decision reevaluateDeferredRemoval(boolean active) {
        this.removalQueued = false;
        return this.setActive(active);
    }

    boolean isRegistered() {
        return this.registered;
    }

    boolean isRemovalQueued() {
        return this.removalQueued;
    }

    enum Decision {
        NONE,
        ADD,
        REMOVE,
        DEFER_REMOVE
    }

}
