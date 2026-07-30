package de.keksuccino.fancymenu.util.rendering.ui.widget.interaction;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuPointerTracker;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.MouseButtonCaptureOwner;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerWidgetInteractionRouterTest {

    @Test
    void consumedLeftClickClaimsFocusAndDragging() {
        TestWidget widget = new TestWidget(true, true, true);
        TestContainer parent = new TestContainer(List.of(widget));
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();

        boolean consumed = ContainerWidgetInteractionRouter.mouseClicked(parent, tracker, parent.children(), 5.0D, 5.0D, 0);

        assertTrue(consumed);
        assertSame(widget, parent.getFocused());
        assertTrue(widget.isFocused());
        assertTrue(parent.isDragging());
        assertEquals(1, widget.clickCount);
    }

    @Test
    void rejectedAndHiddenWidgetsFallThroughWithoutOwnership() {
        TestWidget hiddenWidget = new TestWidget(true, true, true);
        hiddenWidget.visible = false;
        TestWidget rejectingWidget = new TestWidget(false, true, true);
        TestContainer parent = new TestContainer(List.of(hiddenWidget, rejectingWidget));
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();

        boolean consumed = ContainerWidgetInteractionRouter.mouseClicked(parent, tracker, parent.children(), 5.0D, 5.0D, 0);

        assertFalse(consumed);
        assertNull(parent.getFocused());
        assertFalse(parent.isDragging());
        assertEquals(0, hiddenWidget.clickCount);
        assertEquals(1, rejectingWidget.clickCount);
    }

    @Test
    void capturedDragIsConsumedWhenWidgetCallbackReturnsFalse() {
        TestWidget widget = new TestWidget(true, false, true);
        TestContainer parent = new TestContainer(List.of(widget));
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        ContainerWidgetInteractionRouter.mouseClicked(parent, tracker, parent.children(), 5.0D, 5.0D, 0);

        boolean consumed = ContainerWidgetInteractionRouter.mouseDragged(tracker, 7.0D, 5.0D, 0, 2.0D, 0.0D);

        assertTrue(consumed);
        assertEquals(1, widget.dragCount);
        assertTrue(parent.isDragging());
    }

    @Test
    void capturedReleaseForwardsOnceClearsOwnershipAndDraggingAndIsConsumedWhenWidgetReturnsFalse() {
        TestWidget widget = new TestWidget(true, true, false);
        TestContainer parent = new TestContainer(List.of(widget));
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();
        ContainerWidgetInteractionRouter.mouseClicked(parent, tracker, parent.children(), 5.0D, 5.0D, 0);

        boolean firstConsumed = ContainerWidgetInteractionRouter.mouseReleased(parent, tracker, 7.0D, 5.0D, 0);
        boolean secondConsumed = ContainerWidgetInteractionRouter.mouseReleased(parent, tracker, 7.0D, 5.0D, 0);

        assertTrue(firstConsumed);
        assertFalse(secondConsumed);
        assertEquals(1, widget.releaseCount);
        assertFalse(parent.isDragging());
        assertSame(widget, parent.getFocused());
    }

    @Test
    void browserLikeComponentKeepsContainerFocusDragAndReleaseOwnershipOutsideItsBounds() {
        TestBrowserComponent browser = new TestBrowserComponent();
        TestContainer parent = new TestContainer(List.of(browser));
        FancyMenuPointerTracker tracker = new FancyMenuPointerTracker();

        assertTrue(ContainerWidgetInteractionRouter.mouseClicked(parent, tracker, parent.children(), 5.0D, 5.0D, 0));
        assertSame(browser, parent.getFocused());
        assertTrue(browser.isFocused());
        assertTrue(parent.isDragging());
        assertTrue(browser.hasMouseButtonCapture(0));
        assertTrue(ContainerWidgetInteractionRouter.mouseDragged(tracker, 25.0D, 25.0D, 0, 20.0D, 20.0D));
        assertTrue(ContainerWidgetInteractionRouter.mouseReleased(parent, tracker, 25.0D, 25.0D, 0));
        assertEquals(1, browser.dragCount);
        assertEquals(1, browser.releaseCount);
        assertFalse(browser.hasMouseButtonCapture(0));
        assertFalse(parent.isDragging());
    }

    private static final class TestContainer extends AbstractContainerEventHandler {

        private final List<? extends GuiEventListener> children;

        private TestContainer(List<? extends GuiEventListener> children) {
            this.children = children;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

    }

    private static final class TestWidget extends AbstractWidget implements FancyMenuWidget {

        private final boolean clickResult;
        private final boolean dragResult;
        private final boolean releaseResult;
        private int clickCount;
        private int dragCount;
        private int releaseCount;

        private TestWidget(boolean clickResult, boolean dragResult, boolean releaseResult) {
            super(0, 0, 10, 10, Component.empty());
            this.clickResult = clickResult;
            this.dragResult = dragResult;
            this.releaseResult = releaseResult;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.clickCount++;
            return this.clickResult;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.dragCount++;
            return this.dragResult;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCount++;
            return this.releaseResult;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    }

    private static final class TestBrowserComponent extends AbstractWidget implements FancyMenuUiComponent, MouseButtonCaptureOwner {

        private boolean captured;
        private int dragCount;
        private int releaseCount;

        private TestBrowserComponent() {
            super(0, 0, 10, 10, Component.empty());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.captured = this.isMouseOver(mouseX, mouseY);
            return this.captured;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            this.dragCount++;
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCount++;
            this.captured = false;
            return false;
        }

        @Override
        public boolean hasMouseButtonCapture(int button) {
            return this.captured;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

    }

}
