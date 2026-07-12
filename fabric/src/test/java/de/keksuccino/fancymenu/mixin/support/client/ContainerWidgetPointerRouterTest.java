package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerWidgetPointerRouterTest {

    @Test
    void acceptedClickAppliesParentFocusAndPrimaryDragOwnership() {
        TestScreen screen = new TestScreen();
        TestWidget widget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);

        assertTrue(screen.mouseClicked(5.0, 5.0, 0));
        assertSame(widget, screen.focused);
        assertTrue(widget.focused);
        assertTrue(screen.dragging);
        assertEquals(1, widget.clickCount);
    }

    @Test
    void capturedWidgetConsumesDragOutsideItsBoundsRegardlessOfChildResult() {
        TestScreen screen = new TestScreen();
        TestWidget widget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        screen.mouseClicked(5.0, 5.0, 0);

        assertTrue(screen.mouseDragged(100.0, 200.0, 0, 95.0, 195.0));
        assertEquals(1, widget.dragCount);
        assertEquals(100.0, widget.lastMouseX);
        assertEquals(200.0, widget.lastMouseY);
    }

    @Test
    void capturedReleaseIsConsumedRegardlessOfChildResultAndEndsCapture() {
        TestScreen screen = new TestScreen();
        TestWidget widget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        screen.mouseClicked(5.0, 5.0, 0);

        assertTrue(screen.mouseReleased(50.0, 50.0, 0));
        assertEquals(1, widget.releaseCount);
        assertFalse(screen.dragging);
        assertFalse(screen.mouseReleased(50.0, 50.0, 0));
        assertFalse(screen.mouseDragged(50.0, 50.0, 0, 1.0, 1.0));
    }

    @Test
    void capturesDifferentMouseButtonsIndependently() {
        TestScreen screen = new TestScreen();
        TestWidget primaryWidget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        TestWidget secondaryWidget = screen.addWidget(20.0, 0.0, 10.0, 10.0, 1);

        assertTrue(screen.mouseClicked(5.0, 5.0, 0));
        assertTrue(screen.mouseClicked(25.0, 5.0, 1));
        assertTrue(screen.mouseDragged(30.0, 5.0, 1, 5.0, 0.0));
        assertTrue(screen.mouseReleased(30.0, 5.0, 1));
        assertTrue(screen.dragging);
        assertTrue(screen.mouseDragged(15.0, 5.0, 0, 10.0, 0.0));
        assertEquals(1, secondaryWidget.dragCount);
        assertEquals(1, primaryWidget.dragCount);
        assertTrue(screen.mouseReleased(15.0, 5.0, 0));
        assertFalse(screen.dragging);
    }

    @Test
    void hiddenWidgetRejectsClickAndCannotCapturePointer() {
        TestScreen screen = new TestScreen();
        TestWidget widget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        widget.visible = false;

        assertFalse(screen.mouseClicked(5.0, 5.0, 0));
        assertEquals(0, widget.clickCount);
        assertNull(screen.focused);
        assertFalse(screen.dragging);
        assertFalse(screen.mouseDragged(6.0, 6.0, 0, 1.0, 1.0));
        assertFalse(screen.mouseReleased(6.0, 6.0, 0));
    }

    @Test
    void visibleWidgetReturningFalseDoesNotCaptureOrTakeFocus() {
        TestScreen screen = new TestScreen();
        TestWidget widget = screen.addWidget(0.0, 0.0, 10.0, 10.0);

        assertFalse(screen.mouseClicked(5.0, 5.0, 0));
        assertEquals(1, widget.clickCount);
        assertNull(screen.focused);
        assertFalse(screen.dragging);
        assertFalse(screen.mouseDragged(6.0, 6.0, 0, 1.0, 1.0));
        assertFalse(screen.mouseReleased(6.0, 6.0, 0));
    }

    @Test
    void repeatedSameButtonPressReplacesPreviousCapture() {
        TestScreen screen = new TestScreen();
        TestWidget firstWidget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        TestWidget secondWidget = screen.addWidget(20.0, 0.0, 10.0, 10.0, 0);
        screen.mouseClicked(5.0, 5.0, 0);

        assertTrue(screen.mouseClicked(25.0, 5.0, 0));
        assertTrue(screen.mouseReleased(30.0, 5.0, 0));
        assertEquals(0, firstWidget.releaseCount);
        assertEquals(1, secondWidget.releaseCount);
        assertSame(secondWidget, screen.focused);
    }

    @Test
    void rejectedRepeatedPressRemovesPreviousSameButtonCapture() {
        TestScreen screen = new TestScreen();
        TestWidget capturedWidget = screen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        screen.addWidget(20.0, 0.0, 10.0, 10.0);
        screen.mouseClicked(5.0, 5.0, 0);

        assertFalse(screen.mouseClicked(25.0, 5.0, 0));
        assertFalse(screen.dragging);
        assertFalse(screen.mouseDragged(30.0, 5.0, 0, 5.0, 0.0));
        assertFalse(screen.mouseReleased(30.0, 5.0, 0));
        assertEquals(0, capturedWidget.dragCount);
        assertEquals(0, capturedWidget.releaseCount);
    }

    @Test
    void unmatchedDragAndReleaseRemainUnconsumed() {
        TestScreen screen = new TestScreen();

        assertFalse(screen.mouseDragged(5.0, 5.0, 0, 1.0, 1.0));
        assertFalse(screen.mouseReleased(5.0, 5.0, 0));
        assertFalse(screen.mouseDragged(5.0, 5.0, 1, 1.0, 1.0));
        assertFalse(screen.mouseReleased(5.0, 5.0, 1));
    }

    @Test
    void capturesAreIsolatedPerScreenInstance() {
        TestScreen firstScreen = new TestScreen();
        TestScreen secondScreen = new TestScreen();
        TestWidget firstWidget = firstScreen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        TestWidget secondWidget = secondScreen.addWidget(0.0, 0.0, 10.0, 10.0, 0);
        firstScreen.mouseClicked(5.0, 5.0, 0);
        secondScreen.mouseClicked(5.0, 5.0, 0);

        assertTrue(firstScreen.mouseReleased(20.0, 20.0, 0));
        assertFalse(firstScreen.mouseDragged(20.0, 20.0, 0, 1.0, 1.0));
        assertTrue(secondScreen.mouseDragged(20.0, 20.0, 0, 1.0, 1.0));
        assertEquals(1, firstWidget.releaseCount);
        assertEquals(0, firstWidget.dragCount);
        assertEquals(1, secondWidget.dragCount);
        assertEquals(0, secondWidget.releaseCount);
    }

    private static final class TestScreen {

        private final ContainerWidgetPointerRouter<TestWidget> router = new ContainerWidgetPointerRouter<>();
        private final List<TestWidget> widgets = new ArrayList<>();
        private TestWidget focused;
        private boolean dragging;

        private TestWidget addWidget(double x, double y, double width, double height, int... acceptedButtons) {
            TestWidget widget = new TestWidget(x, y, width, height, acceptedButtons);
            this.widgets.add(widget);
            return widget;
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.router.mouseClicked(button, this.widgets, widget -> widget.canClick(mouseX, mouseY), widget -> widget.mouseClicked(mouseX, mouseY, button), this::setFocused, () -> this.dragging = true, () -> this.dragging = false);
        }

        private boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return this.router.mouseDragged(button, widget -> widget.mouseDragged(mouseX, mouseY, button, dragX, dragY));
        }

        private boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.router.mouseReleased(button, widget -> widget.mouseReleased(mouseX, mouseY, button), () -> this.dragging = false);
        }

        private void setFocused(TestWidget widget) {
            if (this.focused != null) this.focused.focused = false;
            this.focused = widget;
            widget.focused = true;
        }

    }

    private static final class TestWidget {

        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final Set<Integer> acceptedButtons = new HashSet<>();
        private boolean active = true;
        private boolean visible = true;
        private boolean focused;
        private int clickCount;
        private int dragCount;
        private int releaseCount;
        private double lastMouseX;
        private double lastMouseY;

        private TestWidget(double x, double y, double width, double height, int... acceptedButtons) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            for (int button : acceptedButtons) this.acceptedButtons.add(button);
        }

        private boolean canClick(double mouseX, double mouseY) {
            return this.active && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.clickCount++;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return this.acceptedButtons.contains(button);
        }

        private boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.dragCount++;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return false;
        }

        private boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCount++;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return false;
        }

    }

}
