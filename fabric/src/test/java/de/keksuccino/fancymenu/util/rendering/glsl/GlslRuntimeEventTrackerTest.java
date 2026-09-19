package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlslRuntimeEventTrackerTest {

    @Test
    void preservesShaderButtonOrderForSdlEventsAndPolling() {
        int[] buttons = {InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOUSE_BUTTON_MIDDLE, 4, 5, 6, 7, 8};
        for (int index = 0; index < buttons.length; index++) {
            int button = buttons[index];
            assertEquals(index, GlslRuntimeEventTracker.shaderButtonIndex(button));
            assertEquals(button, GlslRuntimeEventTracker.nativeMouseButton(index));
            var before = GlslRuntimeEventTracker.snapshot();
            GlslRuntimeEventTracker.onMouseButtonPressed(button, 25, 40);
            var pressed = GlslRuntimeEventTracker.snapshot();
            assertTrue(pressed.mouseButtonStates()[index]);
            assertEquals(before.mouseClickCounts()[index] + 1, pressed.mouseClickCounts()[index]);
            assertEquals(25, pressed.lastMouseClickX()[index]);
            assertEquals(40, pressed.lastMouseClickY()[index]);
            GlslRuntimeEventTracker.onMouseButtonReleased(button, 30, 50);
            var released = GlslRuntimeEventTracker.snapshot();
            assertFalse(released.mouseButtonStates()[index]);
            assertEquals(before.mouseReleaseCounts()[index] + 1, released.mouseReleaseCounts()[index]);
        }
    }

    @Test
    void ignoresUnknownAndUntrackedNativeButtons() {
        var before = GlslRuntimeEventTracker.snapshot();
        for (int button : new int[]{-1, 0, 9, Integer.MAX_VALUE}) {
            GlslRuntimeEventTracker.onMouseButtonPressed(button, 1, 2);
            GlslRuntimeEventTracker.onMouseButtonReleased(button, 1, 2);
        }
        var after = GlslRuntimeEventTracker.snapshot();
        assertArrayEquals(before.mouseButtonStates(), after.mouseButtonStates());
        assertArrayEquals(before.mouseClickCounts(), after.mouseClickCounts());
        assertArrayEquals(before.mouseReleaseCounts(), after.mouseReleaseCounts());
    }

}
