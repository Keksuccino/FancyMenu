package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenOverlayHandlerTest {

    @Test
    void removedClickConsumerOwnsDragAndReleaseUntilRelease() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay lower = new TestOverlay();
        TestOverlay consumer = new TestOverlay();
        AtomicLong consumerId = new AtomicLong();
        consumer.clickHandler = event -> {
            handler.removeOverlay(consumerId.get(), false, true);
            return true;
        };
        handler.addOverlay(lower);
        consumerId.set(handler.addOverlay(consumer));

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));
        assertTrue(handler.mouseReleased(11.0D, 11.0D, 0));

        assertEquals(1, consumer.dragCount);
        assertEquals(1, consumer.releaseCount);
        assertEquals(0, lower.dragCount);
        assertEquals(0, lower.releaseCount);
        assertFalse(handler.mouseDragged(12.0D, 12.0D, 0, 1.0D, 1.0D));
        assertEquals(1, lower.dragCount);
    }

    @Test
    void globalResetDuringClickDetachesPendingCapture() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay consumer = new TestOverlay();
        consumer.clickHandler = event -> {
            handler.clearOverlays();
            return false;
        };
        handler.addOverlay(consumer);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));
        assertTrue(handler.mouseReleased(11.0D, 11.0D, 0));

        assertEquals(0, consumer.dragCount);
        assertEquals(0, consumer.releaseCount);
        assertFalse(handler.mouseDragged(12.0D, 12.0D, 0, 1.0D, 1.0D));
    }

    @Test
    void releaseClearsCaptureBeforeReentrantPress() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay first = new TestOverlay();
        TestOverlay second = new TestOverlay();
        AtomicLong firstId = new AtomicLong();
        first.clickHandler = event -> true;
        first.releaseHandler = event -> {
            handler.removeOverlay(firstId.get(), false, true);
            second.clickHandler = nestedEvent -> true;
            handler.addOverlay(second);
            assertTrue(handler.mouseClicked(event.x(), event.y(), event.button()));
        };
        firstId.set(handler.addOverlay(first));

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 0));
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));
        assertTrue(handler.mouseReleased(11.0D, 11.0D, 0));

        assertEquals(1, first.releaseCount);
        assertEquals(1, second.dragCount);
        assertEquals(1, second.releaseCount);
    }

    @Test
    void capturesAreIndependentPerMouseButton() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay left = new TestOverlay();
        TestOverlay right = new TestOverlay();
        left.clickHandler = event -> event.button() == 0;
        right.clickHandler = event -> event.button() == 1;
        handler.addOverlay(left);
        handler.addOverlay(right);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseClicked(10.0D, 10.0D, 1));
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 0));
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 1, 1.0D, 1.0D));
        assertTrue(handler.mouseReleased(11.0D, 11.0D, 1));

        assertEquals(1, left.releaseCount);
        assertEquals(0, left.dragCount);
        assertEquals(1, right.dragCount);
        assertEquals(1, right.releaseCount);
    }

    @Test
    void uncapturedEventsKeepReverseOverlayOrder() {
        ScreenOverlayHandler handler = createHandler();
        List<String> calls = new ArrayList<>();
        TestOverlay lower = new TestOverlay();
        TestOverlay upper = new TestOverlay();
        lower.dragHandler = event -> {
            calls.add("lower");
            return true;
        };
        upper.dragHandler = event -> {
            calls.add("upper");
            return false;
        };
        handler.addOverlay(lower);
        handler.addOverlay(upper);

        assertTrue(handler.mouseDragged(10.0D, 10.0D, 0, 1.0D, 1.0D));
        assertEquals(List.of("upper", "lower"), calls);
    }

    @Test
    void detachedCaptureConsumesRemainderWithoutRetainingListener() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay consumer = new TestOverlay();
        consumer.clickHandler = event -> true;
        handler.addOverlay(consumer);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        handler.detachMouseCaptures();
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));
        assertTrue(handler.mouseReleased(11.0D, 11.0D, 0));

        assertEquals(0, consumer.dragCount);
        assertEquals(0, consumer.releaseCount);
        assertFalse(handler.mouseReleased(11.0D, 11.0D, 0));
    }

    @Test
    void focusCancellationAndNewPressCannotLeaveStaleCapture() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay first = new TestOverlay();
        TestOverlay second = new TestOverlay();
        first.clickHandler = event -> true;
        second.clickHandler = event -> true;
        handler.addOverlay(first);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        handler.cancelMouseCaptures();
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));
        assertEquals(0, first.dragCount);
        handler.addOverlay(second);
        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 0));

        assertEquals(0, first.releaseCount);
        assertEquals(1, second.releaseCount);
    }

    @Test
    void newPressReplacesCaptureWhenPreviousReleaseWasMissing() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay first = new TestOverlay();
        TestOverlay second = new TestOverlay();
        first.clickHandler = event -> true;
        second.clickHandler = event -> true;
        handler.addOverlay(first);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        handler.addOverlay(second);
        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 0));

        assertEquals(0, first.releaseCount);
        assertEquals(1, second.releaseCount);
    }

    @Test
    void preCanceledNewPressSupersedesDetachedCaptureBeforeDrag() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay oldOwner = new TestOverlay();
        TestOverlay replacement = new TestOverlay();
        oldOwner.clickHandler = event -> true;
        replacement.dragHandler = event -> true;
        long overlayId = handler.addOverlay(oldOwner);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        handler.addOverlayWithId(overlayId, replacement);
        handler.prepareMousePress(0);
        assertTrue(handler.mouseDragged(11.0D, 11.0D, 0, 1.0D, 1.0D));

        assertEquals(0, oldOwner.dragCount);
        assertEquals(1, replacement.dragCount);
    }

    @Test
    void removingOneCapturedOverlayDoesNotDetachAnotherButton() {
        ScreenOverlayHandler handler = createHandler();
        TestOverlay left = new TestOverlay();
        TestOverlay right = new TestOverlay();
        left.clickHandler = event -> event.button() == 0;
        right.clickHandler = event -> event.button() == 1;
        long leftId = handler.addOverlay(left);
        handler.addOverlay(right);

        assertTrue(handler.mouseClicked(10.0D, 10.0D, 0));
        assertTrue(handler.mouseClicked(10.0D, 10.0D, 1));
        handler.removeOverlay(leftId, false, true);
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 0));
        assertTrue(handler.mouseReleased(10.0D, 10.0D, 1));

        assertEquals(0, left.releaseCount);
        assertEquals(1, right.releaseCount);
    }

    private static ScreenOverlayHandler createHandler() {
        return new ScreenOverlayHandler(() -> null);
    }

    private static class TestOverlay implements Renderable, GuiEventListener {

        private Predicate<TestMouseEvent> clickHandler = event -> false;
        private Predicate<TestMouseEvent> dragHandler = event -> false;
        private Consumer<TestMouseEvent> releaseHandler = event -> {};
        private int dragCount;
        private int releaseCount;

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.clickHandler.test(new TestMouseEvent(mouseX, mouseY, button));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.dragCount++;
            return this.dragHandler.test(new TestMouseEvent(mouseX, mouseY, button));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCount++;
            this.releaseHandler.accept(new TestMouseEvent(mouseX, mouseY, button));
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

    private record TestMouseEvent(double x, double y, int button) {
    }

}
