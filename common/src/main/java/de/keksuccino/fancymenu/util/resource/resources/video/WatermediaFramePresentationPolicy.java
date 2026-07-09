package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;

/**
 * Controls whether FancyMenu may expose Watermedia's current frame texture for a player status.
 * Loading states are only safe after a valid frame was presented: Watermedia retains that same
 * texture during seeks, while new players and stopped players reset the presentation flag.
 */
final class WatermediaFramePresentationPolicy {

    private WatermediaFramePresentationPolicy() {}

    static boolean shouldPresentFrame(@NotNull String statusName, boolean playRequested, boolean framePresented) {
        if (!playRequested) return false;
        if (isLoadingStatus(statusName)) return framePresented;
        return !isTerminalStatus(statusName);
    }

    static boolean isLoadingStatus(@NotNull String statusName) {
        return statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING");
    }

    static boolean isTerminalStatus(@NotNull String statusName) {
        return statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR");
    }

}
