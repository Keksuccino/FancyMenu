package de.keksuccino.fancymenu.util.window;

/**
 * Tracks the initial loading-overlay lifecycle and requests one final window-icon refresh when loading ends.
 *
 * <p>One controller belongs to one Minecraft client lifetime. Its completed state is intentionally never reset:
 * later resource-pack reload overlays cannot replace the window icon because vanilla only sets its icon during
 * client construction, so refreshing after them would only repeat file decoding and native icon allocation.</p>
 */
public final class InitialLoadingOverlayIconRefreshController {

    private Phase phase = Phase.WAITING_FOR_INITIAL_LOADING_OVERLAY;

    /**
     * Observes the overlay state after Minecraft accepted and assigned an overlay transition.
     *
     * @param loadingOverlayActive whether the assigned overlay is a loading overlay
     * @return whether the custom window icon must be refreshed for the completed initial loading lifecycle
     */
    public boolean afterOverlayAssignment(boolean loadingOverlayActive) {
        if (this.phase == Phase.WAITING_FOR_INITIAL_LOADING_OVERLAY) {
            if (loadingOverlayActive) {
                this.phase = Phase.INITIAL_LOADING_OVERLAY_ACTIVE;
            }
            return false;
        }
        if (this.phase == Phase.INITIAL_LOADING_OVERLAY_ACTIVE && !loadingOverlayActive) {
            this.phase = Phase.COMPLETE;
            return true;
        }
        return false;
    }

    private enum Phase {
        WAITING_FOR_INITIAL_LOADING_OVERLAY,
        INITIAL_LOADING_OVERLAY_ACTIVE,
        COMPLETE
    }

}
