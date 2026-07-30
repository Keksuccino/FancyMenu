package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FancyMenuPointerTrackerTest {

    @Test
    void focuslessFancyMenuConsumerOwnsDragAndReleaseWithoutFocusInference() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuComponent component = new TestFancyMenuComponent(true, true);

        assertSame(component, tracker.routeMouseClicked(List.of(component), 5.0D, 6.0D, 0));
        assertTrue(tracker.dispatchMouseDragged(5.0D, 6.0D, 0, 2.0D, 3.0D));
        assertTrue(tracker.dispatchMouseReleased(5.0D, 6.0D, 0));
        assertEquals(1, component.clickCalls);
        assertEquals(1, component.dragCalls);
        assertEquals(1, component.releaseCalls);
        assertFalse(tracker.dispatchMouseReleased(5.0D, 6.0D, 0));
        assertEquals(1, component.releaseCalls);
    }

    @Test
    void fancyMenuWidgetsAreOnlyOfferedClicksWhileHovered() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuWidget hiddenWidget = new TestFancyMenuWidget(true, false);
        TestFancyMenuWidget hoveredWidget = new TestFancyMenuWidget(true, true);

        assertSame(hoveredWidget, tracker.routeMouseClicked(List.of(hiddenWidget, hoveredWidget), 5.0D, 6.0D, 0));
        assertEquals(0, hiddenWidget.clickCalls);
        assertEquals(1, hoveredWidget.clickCalls);
    }

    @Test
    void captureOwnerIsRoutedEvenWithoutFancyMenuWidgetMarker() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestCaptureOwner component = new TestCaptureOwner(true, true);

        assertSame(component, tracker.routeMouseClicked(List.of(component), 5.0D, 6.0D, 1));
        assertTrue(tracker.dispatchMouseReleased(5.0D, 6.0D, 1));
        assertEquals(1, component.releaseCalls);
    }

    @Test
    void buttonsKeepIndependentOwnersUntilTheirMatchingReleases() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuComponent left = new TestFancyMenuComponent(true, true, 0);
        TestFancyMenuComponent right = new TestFancyMenuComponent(true, true, 1);

        assertSame(left, tracker.routeMouseClicked(List.of(left, right), 5.0D, 6.0D, 0));
        assertSame(right, tracker.routeMouseClicked(List.of(left, right), 5.0D, 6.0D, 1));
        assertTrue(tracker.dispatchMouseReleased(5.0D, 6.0D, 1));
        assertTrue(tracker.dispatchMouseDragged(5.0D, 6.0D, 0, 1.0D, 1.0D));
        assertTrue(tracker.dispatchMouseReleased(5.0D, 6.0D, 0));
        assertEquals(1, left.dragCalls);
        assertEquals(1, left.releaseCalls);
        assertEquals(1, right.releaseCalls);
    }

    @Test
    void unconsumedRepeatedPressClearsStaleOwnerForThatButton() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuComponent component = new TestFancyMenuComponent(true, true);
        tracker.routeMouseClicked(List.of(component), 5.0D, 6.0D, 0);
        component.consumeClick = false;

        assertNull(tracker.routeMouseClicked(List.of(component), 5.0D, 6.0D, 0));
        assertFalse(tracker.dispatchMouseReleased(5.0D, 6.0D, 0));
        assertEquals(0, component.releaseCalls);
    }

    private static class TestListener implements GuiEventListener {

        boolean consumeClick;
        private final boolean hovered;
        private final int acceptedButton;
        int clickCalls;
        int dragCalls;
        int releaseCalls;
        private boolean focused;

        private TestListener(boolean consumeClick, boolean hovered) {
            this(consumeClick, hovered, -1);
        }

        private TestListener(boolean consumeClick, boolean hovered, int acceptedButton) {
            this.consumeClick = consumeClick;
            this.hovered = hovered;
            this.acceptedButton = acceptedButton;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.clickCalls++;
            return this.consumeClick && ((this.acceptedButton == -1) || (button == this.acceptedButton));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.dragCalls++;
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCalls++;
            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.hovered;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }

    private static final class TestFancyMenuComponent extends TestListener implements FancyMenuUiComponent {

        private TestFancyMenuComponent(boolean consumeClick, boolean hovered) {
            super(consumeClick, hovered);
        }

        private TestFancyMenuComponent(boolean consumeClick, boolean hovered, int acceptedButton) {
            super(consumeClick, hovered, acceptedButton);
        }
    }

    private static final class TestFancyMenuWidget extends TestListener implements FancyMenuWidget {

        private TestFancyMenuWidget(boolean consumeClick, boolean hovered) {
            super(consumeClick, hovered);
        }
    }

    private static final class TestCaptureOwner extends TestListener implements MouseButtonCaptureOwner {

        private TestCaptureOwner(boolean consumeClick, boolean hovered) {
            super(consumeClick, hovered);
        }

        @Override
        public boolean hasMouseButtonCapture(int button) {
            return true;
        }
    }

}
