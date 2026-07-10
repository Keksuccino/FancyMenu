package de.keksuccino.fancymenu.util.rendering;

/**
 * Tracks one full-screen menu-background replacement across legacy screen rendering paths.
 *
 * <p>Some 1.20.1 screens render their background through {@code Screen}, while others render it from a
 * scroll list. The state must therefore be shared by those paths and restored exactly after nested renders.</p>
 */
public final class MenuBackgroundReplacementState {

    private static final int ATTEMPTED_MASK = 1;
    private static final int RENDERED_MASK = 2;

    private boolean attempted;
    private boolean rendered;
    private int wrappedDirtCallDepth;

    /**
     * Starts a render pass and returns an allocation-free snapshot for exact restoration after nested renders.
     */
    public int beginRenderPass() {
        int previousState = (this.attempted ? ATTEMPTED_MASK : 0) | (this.rendered ? RENDERED_MASK : 0);
        this.attempted = false;
        this.rendered = false;
        return previousState;
    }

    public void endRenderPass(int previousState) {
        this.attempted = (previousState & ATTEMPTED_MASK) != 0;
        this.rendered = (previousState & RENDERED_MASK) != 0;
    }

    public boolean isAttempted() {
        return this.attempted;
    }

    public boolean beginAttempt() {
        if (this.attempted) return false;
        this.attempted = true;
        return true;
    }

    public boolean isRendered() {
        return this.rendered;
    }

    public void markRendered() {
        this.attempted = true;
        this.rendered = true;
    }

    public boolean isDirtCallWrapped() {
        return this.wrappedDirtCallDepth > 0;
    }

    public int beginWrappedDirtCall() {
        int previousDepth = this.wrappedDirtCallDepth;
        this.wrappedDirtCallDepth = previousDepth + 1;
        return previousDepth;
    }

    public void endWrappedDirtCall(int previousDepth) {
        this.wrappedDirtCallDepth = previousDepth;
    }

}
