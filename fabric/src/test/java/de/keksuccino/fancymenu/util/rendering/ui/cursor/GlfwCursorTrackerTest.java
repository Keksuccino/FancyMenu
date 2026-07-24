package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlfwCursorTrackerTest {

    @Test
    void destroyedWindowIsNotReturnedForLaterCursorDetachment() {
        long firstWindow = 91001L;
        long secondWindow = 91002L;
        long cursor = 92001L;
        try {
            GlfwCursorTracker.onGlfwSetCursor(firstWindow, cursor);
            GlfwCursorTracker.onGlfwSetCursor(secondWindow, cursor);

            GlfwCursorTracker.onGlfwDestroyWindow(firstWindow);

            assertEquals(Set.of(secondWindow), Set.copyOf(GlfwCursorTracker.getWindowsUsingCursor(cursor)));
        } finally {
            GlfwCursorTracker.onGlfwDestroyWindow(firstWindow);
            GlfwCursorTracker.onGlfwDestroyWindow(secondWindow);
        }
    }

    @Test
    void destroyingActiveCursorTracksGlfwDefaultAndForgetsStandardShape() {
        long window = 91003L;
        long cursor = 92002L;
        try {
            GlfwCursorTracker.onGlfwCreateStandardCursor(123, cursor);
            GlfwCursorTracker.onGlfwSetCursor(window, cursor);

            GlfwCursorTracker.onGlfwDestroyCursor(cursor);

            assertEquals(0L, GlfwCursorTracker.getActiveCursor(window));
            assertEquals(-1, GlfwCursorTracker.getStandardCursorShape(cursor));
        } finally {
            GlfwCursorTracker.onGlfwDestroyCursor(cursor);
            GlfwCursorTracker.onGlfwDestroyWindow(window);
        }
    }

}
