package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementSpaceInputListenerTest {

    // The element spans (10, 20) to (110, 40), so it rotates around (60, 30). Rotated by 90 degrees, screen (60, 70) is local (100, 30).

    @Test
    void clicksReleasesAndScrollsReachTheWidgetInElementSpace() {
        TestElement element = new TestElement(90.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);

        assertTrue(listener.mouseClicked(new MouseButtonEvent(60, 70, new MouseButtonInfo(0, 0)), false));
        assertPoint(100, 30, widget.lastX, widget.lastY);
        assertEquals(0, widget.lastButton);

        listener.mouseReleased(new MouseButtonEvent(110, 70, new MouseButtonInfo(1, 0)));
        assertPoint(100, -20, widget.lastX, widget.lastY);
        assertEquals(1, widget.lastButton);

        listener.mouseScrolled(60, 70, 0, -1);
        assertPoint(100, 30, widget.lastX, widget.lastY);
        assertEquals(-1, widget.lastScrollY);

        listener.mouseMoved(60, -10);
        assertPoint(20, 30, widget.lastX, widget.lastY);
    }

    @Test
    void dragDistancesFollowTheRotation() {
        TestElement element = new TestElement(90.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);

        // Moving down on the screen moves along the local X axis of an element rotated by 90 degrees
        listener.mouseDragged(new MouseButtonEvent(60, 70, new MouseButtonInfo(0, 0)), 0, 5);
        assertPoint(100, 30, widget.lastX, widget.lastY);
        assertPoint(5, 0, widget.lastDragX, widget.lastDragY);
    }

    @Test
    void hoverChecksUseElementSpace() {
        TestElement element = new TestElement(90.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);
        // Screen (60, 70) is local (100, 30), inside the widget area. Screen (100, 30) is local (60, -10), outside of it.
        assertTrue(listener.isMouseOver(60, 70));
        assertFalse(listener.isMouseOver(100, 30));
    }

    @Test
    void unrotatedElementsPassTheSameEvent() {
        TestElement element = new TestElement(0.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);
        MouseButtonEvent event = new MouseButtonEvent(33.5, 21.25, new MouseButtonInfo(0, 0));
        listener.mouseClicked(event, false);
        assertSame(event, widget.lastEvent);
        assertTrue(listener.isMouseOver(60, 30));
        assertFalse(listener.isMouseOver(60, 70));
    }

    @Test
    void listenersKeepTheInputRoutingAndFocusOfTheirWidget() {
        TestElement element = new TestElement(45.0F);
        RecordingWidget plainWidget = new RecordingWidget();
        ElementSpaceInputListener plain = ElementSpaceInputListener.of(element, plainWidget);
        assertFalse(plain instanceof FancyMenuUiComponent);
        assertInstanceOf(FancyMenuUiComponent.class, ElementSpaceInputListener.of(element, new RecordingUiWidget()));

        plain.setFocused(true);
        assertTrue(plainWidget.isFocused());
        assertTrue(plain.isFocused());
        assertSame(plainWidget, plain.getWidget());
    }

    private static void assertPoint(double expectedX, double expectedY, double x, double y) {
        assertEquals(expectedX, x, 1.0E-9);
        assertEquals(expectedY, y, 1.0E-9);
    }

    private static final class TestElement extends AbstractElement {

        private final float rotation;

        private TestElement(float rotation) {
            super(null);
            this.rotation = rotation;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        public float getRotationDegrees() {
            return this.rotation;
        }

        @Override
        public float getVerticalTiltDegrees() {
            return 0.0F;
        }

        @Override
        public float getHorizontalTiltDegrees() {
            return 0.0F;
        }

        @Override
        public int getAbsoluteX() {
            return 10;
        }

        @Override
        public int getAbsoluteY() {
            return 20;
        }

        @Override
        public int getAbsoluteWidth() {
            return 100;
        }

        @Override
        public int getAbsoluteHeight() {
            return 20;
        }

    }

    private static class RecordingWidget implements GuiEventListener, NarratableEntry {

        private double lastX = Double.NaN;
        private double lastY = Double.NaN;
        private double lastDragX = Double.NaN;
        private double lastDragY = Double.NaN;
        private double lastScrollY = Double.NaN;
        private int lastButton = -1;
        private MouseButtonEvent lastEvent;
        private boolean focused;

        private void record(MouseButtonEvent event) {
            this.lastEvent = event;
            this.lastX = event.x();
            this.lastY = event.y();
            this.lastButton = event.button();
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
            this.record(event);
            return true;
        }

        @Override
        public boolean mouseReleased(@NotNull MouseButtonEvent event) {
            this.record(event);
            return true;
        }

        @Override
        public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
            this.record(event);
            this.lastDragX = dragX;
            this.lastDragY = dragY;
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            this.lastX = mouseX;
            this.lastY = mouseY;
            this.lastScrollY = scrollY;
            return true;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            this.lastX = mouseX;
            this.lastY = mouseY;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return (mouseX >= 10) && (mouseX < 110) && (mouseY >= 20) && (mouseY < 40);
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput output) {
        }

    }

    private static final class RecordingUiWidget extends RecordingWidget implements FancyMenuUiComponent {

    }

}
