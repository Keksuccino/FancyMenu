package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks system cursor shapes; SDL exposes the active cursor but not its original shape. */
public final class SdlCursorTracker {

    private static final Map<Long, Integer> STANDARD_CURSOR_SHAPE_BY_CURSOR = new ConcurrentHashMap<>();

    private SdlCursorTracker() {
    }

    public static void onCreateSystemCursor(int shape, long cursor) {
        if (cursor != 0L) STANDARD_CURSOR_SHAPE_BY_CURSOR.put(cursor, shape);
    }

    public static void onDestroyCursor(long cursor) {
        STANDARD_CURSOR_SHAPE_BY_CURSOR.remove(cursor);
    }

    public static int getStandardCursorShape(long cursor) {
        return STANDARD_CURSOR_SHAPE_BY_CURSOR.getOrDefault(cursor, -1);
    }

}
