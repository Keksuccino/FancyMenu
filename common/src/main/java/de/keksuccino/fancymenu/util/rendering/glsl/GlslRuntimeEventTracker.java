package de.keksuccino.fancymenu.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/**
 * Global input event tracker used by FancyMenu GLSL renderers.
 *
 * The tracked data is intentionally lightweight and can be sampled from the render thread
 * every frame. All mutating methods are synchronized because mouse/keyboard callbacks can run
 * between shader draws.
 */
public final class GlslRuntimeEventTracker {

    public static final int KEY_ACTION_RELEASE = 0;
    public static final int KEY_ACTION_PRESS = 1;
    public static final int KEY_ACTION_REPEAT = 2;

    public static final int TRACKED_MOUSE_BUTTONS = 8;

    private static final boolean[] MOUSE_BUTTON_STATES = new boolean[TRACKED_MOUSE_BUTTONS];
    private static final int[] MOUSE_CLICK_COUNTS = new int[TRACKED_MOUSE_BUTTONS];
    private static final int[] MOUSE_RELEASE_COUNTS = new int[TRACKED_MOUSE_BUTTONS];
    private static final double[] LAST_MOUSE_CLICK_X = new double[TRACKED_MOUSE_BUTTONS];
    private static final double[] LAST_MOUSE_CLICK_Y = new double[TRACKED_MOUSE_BUTTONS];
    private static final long[] LAST_MOUSE_CLICK_NANOS = new long[TRACKED_MOUSE_BUTTONS];

    private static double mouseX;
    private static double mouseY;
    private static double mouseDeltaX;
    private static double mouseDeltaY;
    private static double mouseScrollTotalX;
    private static double mouseScrollTotalY;

    private static int keyEventCounter;
    private static int lastKeyCode = -1;
    private static int lastScanCode = -1;
    private static int lastKeyModifiers;
    private static int lastKeyAction = KEY_ACTION_RELEASE;

    private static int charEventCounter;
    private static int lastCharCodePoint = -1;
    private static int lastCharModifiers;

    private GlslRuntimeEventTracker() {
    }

    public static synchronized void onMouseMoved(double x, double y, double deltaX, double deltaY) {
        mouseX = x;
        mouseY = y;
        mouseDeltaX = deltaX;
        mouseDeltaY = deltaY;
    }

    public static synchronized void onMouseButtonPressed(int button, double mouseX, double mouseY) {
        if (!isTrackedMouseButton(button)) {
            return;
        }
        MOUSE_BUTTON_STATES[button] = true;
        MOUSE_CLICK_COUNTS[button]++;
        LAST_MOUSE_CLICK_X[button] = mouseX;
        LAST_MOUSE_CLICK_Y[button] = mouseY;
        LAST_MOUSE_CLICK_NANOS[button] = System.nanoTime();
        GlslRuntimeEventTracker.mouseX = mouseX;
        GlslRuntimeEventTracker.mouseY = mouseY;
    }

    public static synchronized void onMouseButtonReleased(int button, double mouseX, double mouseY) {
        if (!isTrackedMouseButton(button)) {
            return;
        }
        MOUSE_BUTTON_STATES[button] = false;
        MOUSE_RELEASE_COUNTS[button]++;
        GlslRuntimeEventTracker.mouseX = mouseX;
        GlslRuntimeEventTracker.mouseY = mouseY;
    }

    public static synchronized void onMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        GlslRuntimeEventTracker.mouseX = mouseX;
        GlslRuntimeEventTracker.mouseY = mouseY;
        mouseScrollTotalX += scrollDeltaX;
        mouseScrollTotalY += scrollDeltaY;
    }

    /**
     * Reconciles tracked button states with GLFW polling.
     *
     * This prevents "stuck pressed" states if a callback is missed (for example when other
     * handlers cancel flow).
     */
    public static synchronized void syncMouseButtonsFromWindow(long windowPointer) {
        if (windowPointer == 0L) {
            return;
        }
        for (int button = 0; button < TRACKED_MOUSE_BUTTONS; button++) {
            boolean isPressed = GLFW.glfwGetMouseButton(windowPointer, button) == GLFW.GLFW_PRESS;
            boolean wasPressed = MOUSE_BUTTON_STATES[button];
            if (isPressed == wasPressed) {
                continue;
            }
            MOUSE_BUTTON_STATES[button] = isPressed;
            if (isPressed) {
                MOUSE_CLICK_COUNTS[button]++;
                LAST_MOUSE_CLICK_X[button] = mouseX;
                LAST_MOUSE_CLICK_Y[button] = mouseY;
                LAST_MOUSE_CLICK_NANOS[button] = System.nanoTime();
            } else {
                MOUSE_RELEASE_COUNTS[button]++;
            }
        }
    }

    public static synchronized void onKeyPressed(int keyCode, int scanCode, int modifiers, boolean repeated) {
        keyEventCounter++;
        lastKeyCode = keyCode;
        lastScanCode = scanCode;
        lastKeyModifiers = modifiers;
        lastKeyAction = repeated ? KEY_ACTION_REPEAT : KEY_ACTION_PRESS;
    }

    public static synchronized void onKeyReleased(int keyCode, int scanCode, int modifiers) {
        keyEventCounter++;
        lastKeyCode = keyCode;
        lastScanCode = scanCode;
        lastKeyModifiers = modifiers;
        lastKeyAction = KEY_ACTION_RELEASE;
    }

    public static synchronized void onCharTyped(int codePoint, int modifiers) {
        charEventCounter++;
        lastCharCodePoint = codePoint;
        lastCharModifiers = modifiers;
    }

    @NotNull
    public static synchronized InputSnapshot snapshot() {
        return new InputSnapshot(
                mouseX,
                mouseY,
                mouseDeltaX,
                mouseDeltaY,
                mouseScrollTotalX,
                mouseScrollTotalY,
                Arrays.copyOf(MOUSE_BUTTON_STATES, MOUSE_BUTTON_STATES.length),
                Arrays.copyOf(MOUSE_CLICK_COUNTS, MOUSE_CLICK_COUNTS.length),
                Arrays.copyOf(MOUSE_RELEASE_COUNTS, MOUSE_RELEASE_COUNTS.length),
                Arrays.copyOf(LAST_MOUSE_CLICK_X, LAST_MOUSE_CLICK_X.length),
                Arrays.copyOf(LAST_MOUSE_CLICK_Y, LAST_MOUSE_CLICK_Y.length),
                Arrays.copyOf(LAST_MOUSE_CLICK_NANOS, LAST_MOUSE_CLICK_NANOS.length),
                keyEventCounter,
                lastKeyCode,
                lastScanCode,
                lastKeyModifiers,
                lastKeyAction,
                charEventCounter,
                lastCharCodePoint,
                lastCharModifiers
        );
    }

    private static boolean isTrackedMouseButton(int button) {
        return button >= 0 && button < TRACKED_MOUSE_BUTTONS;
    }

    public record InputSnapshot(
            double mouseX,
            double mouseY,
            double mouseDeltaX,
            double mouseDeltaY,
            double mouseScrollTotalX,
            double mouseScrollTotalY,
            @NotNull boolean[] mouseButtonStates,
            @NotNull int[] mouseClickCounts,
            @NotNull int[] mouseReleaseCounts,
            @NotNull double[] lastMouseClickX,
            @NotNull double[] lastMouseClickY,
            @NotNull long[] lastMouseClickNanos,
            int keyEventCounter,
            int lastKeyCode,
            int lastScanCode,
            int lastKeyModifiers,
            int lastKeyAction,
            int charEventCounter,
            int lastCharCodePoint,
            int lastCharModifiers
    ) {
    }

}
