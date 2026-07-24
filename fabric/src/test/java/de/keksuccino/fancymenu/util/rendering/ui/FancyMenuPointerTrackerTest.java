package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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
        MouseButtonEvent event = mouseEvent(0);

        assertSame(component, tracker.routeMouseClicked(List.of(component), event, false));
        assertTrue(tracker.dispatchMouseDragged(event, 2.0D, 3.0D));
        assertTrue(tracker.dispatchMouseReleased(event));
        assertEquals(1, component.clickCalls);
        assertEquals(1, component.dragCalls);
        assertEquals(1, component.releaseCalls);
        assertFalse(tracker.dispatchMouseReleased(event));
        assertEquals(1, component.releaseCalls);
    }

    @Test
    void fancyMenuWidgetsAreOnlyOfferedClicksWhileHovered() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuWidget hiddenWidget = new TestFancyMenuWidget(true, false);
        TestFancyMenuWidget hoveredWidget = new TestFancyMenuWidget(true, true);

        assertSame(hoveredWidget, tracker.routeMouseClicked(List.of(hiddenWidget, hoveredWidget), mouseEvent(0), false));
        assertEquals(0, hiddenWidget.clickCalls);
        assertEquals(1, hoveredWidget.clickCalls);
    }

    @Test
    void captureOwnerIsRoutedEvenWithoutFancyMenuWidgetMarker() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestCaptureOwner component = new TestCaptureOwner(true, true);
        MouseButtonEvent event = mouseEvent(1);

        assertSame(component, tracker.routeMouseClicked(List.of(component), event, false));
        assertTrue(tracker.dispatchMouseReleased(event));
        assertEquals(1, component.releaseCalls);
    }

    @Test
    void buttonsKeepIndependentOwnersUntilTheirMatchingReleases() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuComponent left = new TestFancyMenuComponent(true, true, 0);
        TestFancyMenuComponent right = new TestFancyMenuComponent(true, true, 1);
        MouseButtonEvent leftEvent = mouseEvent(0);
        MouseButtonEvent rightEvent = mouseEvent(1);

        assertSame(left, tracker.routeMouseClicked(List.of(left, right), leftEvent, false));
        assertSame(right, tracker.routeMouseClicked(List.of(left, right), rightEvent, false));
        assertTrue(tracker.dispatchMouseReleased(rightEvent));
        assertTrue(tracker.dispatchMouseDragged(leftEvent, 1.0D, 1.0D));
        assertTrue(tracker.dispatchMouseReleased(leftEvent));
        assertEquals(1, left.dragCalls);
        assertEquals(1, left.releaseCalls);
        assertEquals(1, right.releaseCalls);
    }

    @Test
    void unconsumedRepeatedPressClearsStaleOwnerForThatButton() {
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        TestFancyMenuComponent component = new TestFancyMenuComponent(true, true);
        MouseButtonEvent event = mouseEvent(0);
        tracker.routeMouseClicked(List.of(component), event, false);
        component.consumeClick = false;

        assertNull(tracker.routeMouseClicked(List.of(component), event, false));
        assertFalse(tracker.dispatchMouseReleased(event));
        assertEquals(0, component.releaseCalls);
    }

    private static MouseButtonEvent mouseEvent(int button) {
        return new MouseButtonEvent(5.0D, 6.0D, new MouseButtonInfo(button, 0));
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
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            this.clickCalls++;
            return this.consumeClick && ((this.acceptedButton == -1) || (event.button() == this.acceptedButton));
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            this.dragCalls++;
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
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
