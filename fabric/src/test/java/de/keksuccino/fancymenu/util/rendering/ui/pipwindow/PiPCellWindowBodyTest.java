package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiPCellWindowBodyTest {

    @Test
    void wheelEventFallsThroughCellToScrollAreaAndStopsAfterConsumption() {
        TestBody screen = new TestBody();
        TestListener cell = new TestListener(false);
        TestListener scrollArea = new TestListener(true);
        TestListener laterWidget = new TestListener(true);
        screen.addTestChild(cell);
        screen.addTestChild(scrollArea);
        screen.addTestChild(laterWidget);

        boolean handled = screen.mouseScrolled(12.5D, 24.5D, -1.25D, 2.75D);

        assertTrue(handled);
        assertEquals(1, cell.callCount);
        assertEquals(1, scrollArea.callCount);
        assertEquals(0, laterWidget.callCount);
        assertEquals(12.5D, scrollArea.mouseX);
        assertEquals(24.5D, scrollArea.mouseY);
        assertEquals(-1.25D, scrollArea.scrollDeltaX);
        assertEquals(2.75D, scrollArea.scrollDeltaY);
    }

    @Test
    void firstConsumingChildShortCircuitsDispatch() {
        TestBody screen = new TestBody();
        TestListener first = new TestListener(true);
        TestListener second = new TestListener(true);
        screen.addTestChild(first);
        screen.addTestChild(second);

        boolean handled = screen.mouseScrolled(0.0D, 0.0D, 0.0D, -1.0D);

        assertTrue(handled);
        assertEquals(1, first.callCount);
        assertEquals(0, second.callCount);
    }

    @Test
    void unconsumedWheelEventVisitsEveryChildAndReturnsFalse() {
        TestBody screen = new TestBody();
        TestListener first = new TestListener(false);
        TestListener second = new TestListener(false);
        screen.addTestChild(first);
        screen.addTestChild(second);

        boolean handled = screen.mouseScrolled(1.0D, 2.0D, 3.0D, 4.0D);

        assertFalse(handled);
        assertEquals(1, first.callCount);
        assertEquals(1, second.callCount);
    }

    private static final class TestBody extends PiPCellWindowBody {

        private void addTestChild(TestListener listener) {
            this.addWidget(listener);
        }

        @Override
        protected void onCancel() {
        }

        @Override
        protected void onDone() {
        }

    }

    private static final class TestListener implements GuiEventListener, NarratableEntry {

        private final boolean handled;
        private int callCount;
        private double mouseX;
        private double mouseY;
        private double scrollDeltaX;
        private double scrollDeltaY;

        private TestListener(boolean handled) {
            this.handled = handled;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.callCount++;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.scrollDeltaX = scrollDeltaX;
            this.scrollDeltaY = scrollDeltaY;
            return this.handled;
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
        }

    }

}
