package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementSpaceInputListenerTest {

    // The element spans (10, 20) to (110, 40), so it rotates around (60, 30). Rotated by 90 degrees, screen (60, 70) is local (100, 30).

    @Test
    void clicksReleasesAndScrollsReachTheWidgetInElementSpace() {
        TestElement element = new TestElement(90.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);

        assertTrue(listener.mouseClicked(60, 70, 0));
        assertPoint(100, 30, widget.lastX, widget.lastY);
        assertEquals(0, widget.lastButton);

        listener.mouseReleased(110, 70, 1);
        assertPoint(100, -20, widget.lastX, widget.lastY);
        assertEquals(1, widget.lastButton);

        listener.mouseScrolled(60, 70, -1);
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
        listener.mouseDragged(60, 70, 0, 0, 5);
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
    void unrotatedElementsPassTheSamePosition() {
        TestElement element = new TestElement(0.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.of(element, widget);
        listener.mouseClicked(33.5, 21.25, 0);
        assertPoint(33.5, 21.25, widget.lastX, widget.lastY);
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
        assertTrue(plainWidget.focused);
        assertTrue(plain.isFocused());
        assertSame(plainWidget, ElementSpaceInputListener.unwrap(plain));
    }

    @Test
    void widgetsThatWorkOutTheirOwnRotationGetTheMouseWithIt() {
        TestElement element = new TestElement(90.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.ofChildWidget(element, widget);

        assertTrue(listener.mouseClicked(60, 70, 0));
        assertPoint(60, 70, widget.lastX, widget.lastY);
        listener.mouseDragged(60, 70, 0, 0, 5);
        assertPoint(0, 5, widget.lastDragX, widget.lastDragY);
        assertInstanceOf(FancyMenuUiComponent.class, ElementSpaceInputListener.ofChildWidget(element, new RecordingUiWidget()));
    }

    @Test
    void theListenerIsWhatTheScreenGivesTheFocusTo() {
        TestElement element = new TestElement(0.0F);
        RecordingWidget widget = new RecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.ofChildWidget(element, widget);

        assertNull(listener.getCurrentFocusPath());
        ComponentPath path = listener.nextFocusPath(new FocusNavigationEvent.TabNavigation(true));
        assertNotNull(path);
        // The path ends at the listener, which is what the screen has in its list of children
        assertSame(listener, path.component());
        path.applyFocus(true);
        assertTrue(widget.isFocused());
        ComponentPath current = listener.getCurrentFocusPath();
        assertNotNull(current);
        assertSame(listener, current.component());
        // A widget that has the focus does not take it again
        assertNull(listener.nextFocusPath(new FocusNavigationEvent.TabNavigation(true)));
    }

    @Test
    void theListenerIsAsFocusableAndNavigatableAsItsWidget() {
        TestElement element = new TestElement(0.0F);
        ElementSpaceInputListener plain = ElementSpaceInputListener.ofChildWidget(element, new RecordingWidget());
        assertTrue(plain.isFocusable());
        assertTrue(plain.isNavigatable());

        NavigatableRecordingWidget widget = new NavigatableRecordingWidget();
        ElementSpaceInputListener listener = ElementSpaceInputListener.ofChildWidget(element, widget);
        assertTrue(listener.isFocusable());
        widget.focusable = false;
        assertFalse(listener.isFocusable());
        assertTrue(listener.isNavigatable());
        listener.setNavigatable(false);
        assertFalse(widget.navigatable);
        assertFalse(listener.isNavigatable());
    }

    @Test
    void unwrappingGivesTheWidgetBehindAListener() {
        TestElement element = new TestElement(0.0F);
        RecordingWidget widget = new RecordingWidget();
        assertSame(widget, ElementSpaceInputListener.unwrap(ElementSpaceInputListener.of(element, widget)));
        assertSame(widget, ElementSpaceInputListener.unwrap(widget));
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
        private boolean focused;

        private void record(double mouseX, double mouseY, int button) {
            this.lastX = mouseX;
            this.lastY = mouseY;
            this.lastButton = button;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.record(mouseX, mouseY, button);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.record(mouseX, mouseY, button);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.record(mouseX, mouseY, button);
            this.lastDragX = dragX;
            this.lastDragY = dragY;
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            this.lastX = mouseX;
            this.lastY = mouseY;
            this.lastScrollY = delta;
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

        /** Like a button: it takes the focus the keyboard moves on, unless it has it already. */
        @Override
        public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigationEvent) {
            return this.focused ? null : ComponentPath.leaf(this);
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

    private static final class NavigatableRecordingWidget extends RecordingWidget implements NavigatableWidget {

        private boolean focusable = true;
        private boolean navigatable = true;

        @Override
        public boolean isFocusable() {
            return this.focusable;
        }

        @Override
        public void setFocusable(boolean focusable) {
            this.focusable = focusable;
        }

        @Override
        public boolean isNavigatable() {
            return this.navigatable;
        }

        @Override
        public void setNavigatable(boolean navigatable) {
            this.navigatable = navigatable;
        }

    }

}
