package de.keksuccino.fancymenu.util.window;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Gets a frame with the new window size on screen after a fullscreen switch or window resize during the loading overlay,
 * before Minecraft runs its queued main thread tasks again.<br><br>
 *
 * Minecraft runs its queued tasks ({@code Minecraft#runAllTasks()}) in every frame before rendering. During the initial
 * resource reload, mod loaders run the setup of mods through that queue, which can block the render thread for a second or longer
 * in big modpacks. The new framebuffer size of a fullscreen switch can arrive frames after the switch, through the size callback of the
 * window, so without this gate, the frame on screen during such a freeze can still have the old window size and sit in the corner of the
 * otherwise black fullscreen window.<br><br>
 *
 * The gate closes when the window switches between fullscreen and windowed mode, or when the framebuffer size changes,
 * while the loading overlay is active. While it is closed, the game skips {@code runAllTasks()} (and only that, ticking and rendering
 * go on), until a frame with a size other than the one on screen during the switch or resize is about to be presented,
 * or until it held back the tasks for {@link #TIMEOUT_NANOS}. A switch that keeps the framebuffer size (same resolution) only ends by
 * the timeout. The timeout starts when the gate holds back the tasks for the first time, because handling the switch or resize itself
 * (like resizing the current screen) can take longer than the timeout.<br>
 * Limits that keep the gate from ever stalling the loading:
 * <ul>
 *     <li>After a frame with the new size opened the gate, it stays open until the tasks ran once,
 *     so a window that gets resized every frame only holds back the tasks every other frame.</li>
 *     <li>After a timeout, the gate stays open until the loading overlay is gone,
 *     so a window that keeps switching without changing its size only holds back the tasks once.</li>
 *     <li>The gate never closes outside the loading overlay, so normal window resizing stays untouched.</li>
 * </ul>
 * All methods have to get called on the render thread.
 */
public final class LoadingOverlayResizeTaskGate {

    public static final LoadingOverlayResizeTaskGate INSTANCE = new LoadingOverlayResizeTaskGate(System::nanoTime);

    static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private final LongSupplier nanoClock;
    private State state = State.OPEN;
    private boolean holdingTasks;
    private long holdDeadlineNanos;
    private int staleFrameWidth;
    private int staleFrameHeight;
    private boolean framePresented;
    private int lastFrameWidth;
    private int lastFrameHeight;

    LoadingOverlayResizeTaskGate(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    /**
     * Has to get called right before the window switches between fullscreen and windowed mode.
     *
     * @param loadingOverlayActive whether the loading overlay is active
     * @param framebufferWidth the framebuffer width before the switch
     * @param framebufferHeight the framebuffer height before the switch
     */
    public void onFullscreenSwitch(boolean loadingOverlayActive, int framebufferWidth, int framebufferHeight) {
        if (loadingOverlayActive) this.close(framebufferWidth, framebufferHeight);
    }

    /**
     * Has to get called when Minecraft resizes its display, which also happens when only the GUI scale changes.
     *
     * @param loadingOverlayActive whether the loading overlay is active
     * @param framebufferWidth the current framebuffer width
     * @param framebufferHeight the current framebuffer height
     */
    public void onDisplayResized(boolean loadingOverlayActive, int framebufferWidth, int framebufferHeight) {
        // Nothing is on screen yet that could have the wrong size
        if (!loadingOverlayActive || !this.framePresented) return;
        if ((framebufferWidth == this.lastFrameWidth) && (framebufferHeight == this.lastFrameHeight)) return;
        this.close(this.lastFrameWidth, this.lastFrameHeight);
    }

    /**
     * Has to get called for every finished frame right before it gets presented.<br>
     * Presenting can poll window events, which resize the display. Calling this before presenting makes the frame that was just rendered
     * count as the frame on screen for these resizes.
     *
     * @param frameWidth the width the frame got rendered at
     * @param frameHeight the height the frame got rendered at
     */
    public void onFramePresenting(int frameWidth, int frameHeight) {
        this.framePresented = true;
        this.lastFrameWidth = frameWidth;
        this.lastFrameHeight = frameHeight;
        if ((this.state == State.CLOSED) && ((frameWidth != this.staleFrameWidth) || (frameHeight != this.staleFrameHeight))) {
            this.state = State.OPEN_UNTIL_TASKS_RAN;
        }
    }

    /**
     * Has to get called right before Minecraft would run its queued main thread tasks in a frame.
     *
     * @param loadingOverlayActive whether the loading overlay is active
     * @return whether Minecraft may run its queued tasks now
     */
    public boolean shouldRunTasks(boolean loadingOverlayActive) {
        if (!loadingOverlayActive) {
            this.state = State.OPEN;
            return true;
        }
        if (this.state == State.OPEN_UNTIL_TASKS_RAN) {
            this.state = State.OPEN;
        } else if (this.state == State.CLOSED) {
            long now = this.nanoClock.getAsLong();
            if (!this.holdingTasks) {
                this.holdingTasks = true;
                this.holdDeadlineNanos = now + TIMEOUT_NANOS;
                return false;
            }
            if ((now - this.holdDeadlineNanos) < 0L) return false;
            this.state = State.OPEN_UNTIL_OVERLAY_ENDS;
        }
        return true;
    }

    private void close(int staleWidth, int staleHeight) {
        if (this.state == State.OPEN) {
            this.state = State.CLOSED;
            this.holdingTasks = false;
        }
        if (this.state == State.CLOSED) {
            this.staleFrameWidth = staleWidth;
            this.staleFrameHeight = staleHeight;
        }
    }

    private enum State {
        OPEN,
        CLOSED,
        OPEN_UNTIL_TASKS_RAN,
        OPEN_UNTIL_OVERLAY_ENDS
    }

}
