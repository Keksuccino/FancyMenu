package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the currently set cursor on GLFW level.<br>
 * GLFW has no getter for the active cursor, so we store the last cursor handle that was set per window.
 */
public final class GlfwCursorTracker {

    private static final Map<Long, Long> ACTIVE_CURSOR_BY_WINDOW = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> STANDARD_CURSOR_SHAPE_BY_CURSOR = new ConcurrentHashMap<>();

    private GlfwCursorTracker() {
    }

    public static void onGlfwSetCursor(long window, long cursor) {
        ACTIVE_CURSOR_BY_WINDOW.put(window, cursor);
    }

    public static void onGlfwCreateStandardCursor(int shape, long cursor) {
        if (cursor != 0L) {
            STANDARD_CURSOR_SHAPE_BY_CURSOR.put(cursor, shape);
        }
    }

    public static void onGlfwDestroyCursor(long cursor) {
        STANDARD_CURSOR_SHAPE_BY_CURSOR.remove(cursor);
    }

    public static long getActiveCursor(long window) {
        Long cursor = ACTIVE_CURSOR_BY_WINDOW.get(window);
        return cursor != null ? cursor : -1L;
    }

    public static int getStandardCursorShape(long cursor) {
        Integer shape = STANDARD_CURSOR_SHAPE_BY_CURSOR.get(cursor);
        return shape != null ? shape : -1;
    }

    public static int getActiveStandardCursorShape(long window) {
        return getStandardCursorShape(getActiveCursor(window));
    }

    @NotNull
    public static Map<Long, Long> getActiveCursorByWindowView() {
        return ACTIVE_CURSOR_BY_WINDOW;
    }

}
