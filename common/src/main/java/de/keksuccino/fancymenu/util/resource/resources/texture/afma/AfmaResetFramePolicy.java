package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

final class AfmaResetFramePolicy {

    private AfmaResetFramePolicy() {
    }

    static boolean shouldSkipFrameApplication(boolean restoredSnapshot, boolean intro, int frameIndex, int introFrameCount) {
        return restoredSnapshot && (frameIndex == 0) && (intro || (introFrameCount <= 0));
    }

}
