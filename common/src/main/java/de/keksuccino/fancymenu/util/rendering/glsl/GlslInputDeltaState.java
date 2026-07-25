package de.keksuccino.fancymenu.util.rendering.glsl;

/** Per-runtime input baselines that prevent persistent global tracker values from replaying on later frames. */
final class GlslInputDeltaState {

    private boolean initialized;
    private double scrollTotalX;
    private double scrollTotalY;
    private int mouseMoveEventCounter;

    Delta capture(double currentScrollTotalX, double currentScrollTotalY, double currentMouseDeltaX, double currentMouseDeltaY, int currentMouseMoveEventCounter, boolean enabled) {
        if (!this.initialized || !enabled) {
            this.initialized = true;
            this.scrollTotalX = currentScrollTotalX;
            this.scrollTotalY = currentScrollTotalY;
            this.mouseMoveEventCounter = currentMouseMoveEventCounter;
            return Delta.ZERO;
        }

        double scrollDeltaX = currentScrollTotalX - this.scrollTotalX;
        double scrollDeltaY = currentScrollTotalY - this.scrollTotalY;
        double mouseDeltaX = currentMouseMoveEventCounter != this.mouseMoveEventCounter ? currentMouseDeltaX : 0.0D;
        double mouseDeltaY = currentMouseMoveEventCounter != this.mouseMoveEventCounter ? currentMouseDeltaY : 0.0D;
        this.scrollTotalX = currentScrollTotalX;
        this.scrollTotalY = currentScrollTotalY;
        this.mouseMoveEventCounter = currentMouseMoveEventCounter;
        return new Delta(scrollDeltaX, scrollDeltaY, mouseDeltaX, mouseDeltaY);
    }

    void reset() {
        this.initialized = false;
        this.scrollTotalX = 0.0D;
        this.scrollTotalY = 0.0D;
        this.mouseMoveEventCounter = 0;
    }

    record Delta(double scrollX, double scrollY, double mouseX, double mouseY) {

        private static final Delta ZERO = new Delta(0.0D, 0.0D, 0.0D, 0.0D);
    }
}
