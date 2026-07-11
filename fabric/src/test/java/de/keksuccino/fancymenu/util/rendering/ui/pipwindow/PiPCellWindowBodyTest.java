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
    void decliningChildDoesNotBlockLaterHandler() {
        TestBody body = new TestBody();
        TestListener declining = new TestListener(false);
        TestListener handling = new TestListener(true);
        body.addTestChild(declining);
        body.addTestChild(handling);

        assertTrue(body.mouseScrolled(12.5D, 23.75D, -1.5D));
        assertEquals(1, declining.callCount);
        assertEquals(1, handling.callCount);
        assertEquals(12.5D, handling.mouseX);
        assertEquals(23.75D, handling.mouseY);
        assertEquals(-1.5D, handling.scrollDeltaY);
    }

    @Test
    void firstHandlerStopsFurtherDispatch() {
        TestBody body = new TestBody();
        TestListener handling = new TestListener(true);
        TestListener unreachable = new TestListener(true);
        body.addTestChild(handling);
        body.addTestChild(unreachable);

        assertTrue(body.mouseScrolled(1.0D, 2.0D, 3.0D));
        assertEquals(1, handling.callCount);
        assertEquals(0, unreachable.callCount);
    }

    @Test
    void allDecliningChildrenReturnFalse() {
        TestBody body = new TestBody();
        TestListener first = new TestListener(false);
        TestListener second = new TestListener(false);
        body.addTestChild(first);
        body.addTestChild(second);

        assertFalse(body.mouseScrolled(4.0D, 5.0D, 6.0D));
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

        private final boolean handlesScroll;
        private int callCount;
        private double mouseX;
        private double mouseY;
        private double scrollDeltaY;

        private TestListener(boolean handlesScroll) {
            this.handlesScroll = handlesScroll;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaY) {
            this.callCount++;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.scrollDeltaY = scrollDeltaY;
            return this.handlesScroll;
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
