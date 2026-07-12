package de.keksuccino.fancymenu.util.resource.resources.video;

import javax.annotation.Nonnull;

final class WatermediaFramePresentationPolicy {

    private WatermediaFramePresentationPolicy() {
    }

    static boolean shouldPresentFrame(boolean playRequested, boolean framePresented, @Nonnull String statusName) {
        if (!playRequested) return false;
        if (isLoadingPlayerStatus(statusName)) {
            // Watermedia keeps its last GL texture alive while repeat seeks and ordinary seeks temporarily enter BUFFERING. Preserve that frame once one was presented so renderers do not expose their fallback between decoded frames.
            return framePresented;
        }
        return !isTerminalPlayerStatus(statusName);
    }

    static boolean isLoadingPlayerStatus(@Nonnull String statusName) {
        return statusName.equals("WAITING") || statusName.equals("LOADING") || statusName.equals("BUFFERING");
    }

    static boolean isTerminalPlayerStatus(@Nonnull String statusName) {
        return statusName.equals("STOPPED") || statusName.equals("ENDED") || statusName.equals("ERROR");
    }

}
