package de.keksuccino.fancymenu.util.resource.resources.video;

/**
 * Controls whether FancyMenu may expose Watermedia's current frame texture for a player status.
 * Loading states are only safe after a valid frame was presented: Watermedia retains that same
 * texture during seeks, while new players and stopped players reset the presentation flag.
 */
final class WatermediaFramePresentationPolicy {

    private WatermediaFramePresentationPolicy() {
    }

    static boolean shouldPresentFrame(boolean playRequested, boolean framePresented, boolean loadingPlayerStatus, boolean terminalPlayerStatus) {
        if (!playRequested) return false;
        if (loadingPlayerStatus) return framePresented;
        return !terminalPlayerStatus;
    }

    static boolean isLoadingPlayerStatus(String statusName) {
        return statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING");
    }

    static boolean isTerminalPlayerStatus(String statusName) {
        return statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR");
    }

}
