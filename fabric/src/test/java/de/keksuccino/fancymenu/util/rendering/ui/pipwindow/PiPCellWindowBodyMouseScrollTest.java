package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiPCellWindowBodyMouseScrollTest {

    @Test
    void continuesAfterUnhandledChildAndReturnsLaterHandledResult() {
        TestListener first = new TestListener(false);
        TestListener second = new TestListener(true);

        assertTrue(PiPCellWindowBody.dispatchMouseScrolled(List.of(first, second), 1.0D, 2.0D, 3.0D, 4.0D));
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
    }

    @Test
    void stopsAtFirstHandledChild() {
        TestListener first = new TestListener(true);
        TestListener second = new TestListener(true);

        assertTrue(PiPCellWindowBody.dispatchMouseScrolled(List.of(first, second), 1.0D, 2.0D, 3.0D, 4.0D));
        assertEquals(1, first.calls);
        assertEquals(0, second.calls);
    }

    @Test
    void returnsFalseWhenNoChildHandlesScrolling() {
        TestListener first = new TestListener(false);
        TestListener second = new TestListener(false);

        assertFalse(PiPCellWindowBody.dispatchMouseScrolled(List.of(first, second), 1.0D, 2.0D, 3.0D, 4.0D));
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
    }

    @Test
    void forwardsCoordinatesAndBothScrollDeltasExactly() {
        TestListener listener = new TestListener(true);

        assertTrue(PiPCellWindowBody.dispatchMouseScrolled(List.of(listener), 12.25D, -7.5D, 0.125D, -3.75D));
        assertEquals(12.25D, listener.mouseX);
        assertEquals(-7.5D, listener.mouseY);
        assertEquals(0.125D, listener.scrollDeltaX);
        assertEquals(-3.75D, listener.scrollDeltaY);
    }

    private static final class TestListener implements GuiEventListener {

        private final boolean handled;
        private int calls;
        private double mouseX;
        private double mouseY;
        private double scrollDeltaX;
        private double scrollDeltaY;

        private TestListener(boolean handled) {
            this.handled = handled;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.calls++;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.scrollDeltaX = scrollDeltaX;
            this.scrollDeltaY = scrollDeltaY;
            return this.handled;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

}
