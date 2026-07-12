package de.keksuccino.fancymenu.util.resource.resources.video;

final class Mp4VideoFramePresentationPolicy {

    private Mp4VideoFramePresentationPolicy() {
    }

    static boolean shouldPresentFrame(boolean playRequested, boolean framePresented, boolean loadingStatus, boolean terminalStatus) {
        if (!playRequested) return false;
        if (loadingStatus) {
            // Watermedia keeps its last GL texture alive while repeat seeks and ordinary seeks temporarily enter BUFFERING. Preserve that frame once one was presented so renderers do not expose their fallback between decoded frames.
            return framePresented;
        }
        return !terminalStatus;
    }

}
