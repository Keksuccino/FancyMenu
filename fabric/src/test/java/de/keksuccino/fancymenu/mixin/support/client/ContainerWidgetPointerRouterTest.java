package de.keksuccino.fancymenu.mixin.support.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerWidgetPointerRouterTest {

    @Test
    void handledLeftClickAssignsFocusDraggingAndPointerOwnership() {
        TestWidget widget = new TestWidget(0, true, true);
        TestHost host = new TestHost(widget);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertFalse(widget.isHovered());
        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertSame(widget, host.focused);
        assertTrue(host.dragging);
        assertTrue(router.mouseDragged(event(0, 6.0D, 6.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(0, 6.0D, 6.0D)));
        assertFalse(host.dragging);
        assertEquals(1, widget.dragCalls);
        assertEquals(1, widget.releaseCalls);
    }

    @Test
    void handledNonFocusClickOwnsPointerWithoutChangingFocusOrDragging() {
        TestWidget widget = new TestWidget(0, true, false);
        TestHost host = new TestHost(widget);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertNull(host.focused);
        assertFalse(host.dragging);
        assertTrue(router.mouseDragged(event(0, 6.0D, 6.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(0, 6.0D, 6.0D)));
    }

    @Test
    void invisibleInactiveOutsideAndUnhandledWidgetsDoNotAcquireOwnership() {
        TestWidget invisible = new TestWidget(0, true, true);
        invisible.visible = false;
        TestWidget inactive = new TestWidget(0, true, true);
        inactive.active = false;
        TestWidget outside = new TestWidget(0, true, true);
        outside.setX(20);
        TestWidget unhandled = new TestWidget(0, false, true);
        TestHost host = new TestHost(invisible, inactive, outside, unhandled);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertFalse(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertEquals(0, invisible.clickCalls);
        assertEquals(0, inactive.clickCalls);
        assertEquals(0, outside.clickCalls);
        assertEquals(1, unhandled.clickCalls);
        assertFalse(router.mouseDragged(event(0, 6.0D, 6.0D), 1.0D, 1.0D));
        assertFalse(router.mouseReleased(host, event(0, 6.0D, 6.0D)));
    }

    @Test
    void dragAndReleaseRouteOnlyToTheExactButtonOwner() {
        TestWidget widget = new TestWidget(1, true, true);
        TestHost host = new TestHost(widget);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(1, 5.0D, 5.0D), false));
        assertFalse(router.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertFalse(router.mouseReleased(host, event(0, 5.0D, 5.0D)));
        assertEquals(0, widget.dragCalls);
        assertEquals(0, widget.releaseCalls);
        assertTrue(router.mouseDragged(event(1, 5.0D, 5.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(1, 5.0D, 5.0D)));
        assertEquals(1, widget.dragCalls);
        assertEquals(1, widget.releaseCalls);
    }

    @Test
    void ownedDragAndReleaseAreConsumedWhenWidgetCallbacksReturnFalse() {
        TestWidget widget = new TestWidget(0, true, true);
        widget.dragHandled = false;
        widget.releaseHandled = false;
        TestHost host = new TestHost(widget);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertTrue(router.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(0, 5.0D, 5.0D)));
        assertEquals(1, widget.dragCalls);
        assertEquals(1, widget.releaseCalls);
    }

    @Test
    void newSameButtonPressReplacesTheStaleOwner() {
        TestWidget first = new TestWidget(0, true, true);
        TestWidget second = new TestWidget(0, true, true);
        TestHost host = new TestHost(first);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        host.children = List.of(second);
        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertTrue(router.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(0, 5.0D, 5.0D)));
        assertEquals(0, first.dragCalls);
        assertEquals(0, first.releaseCalls);
        assertEquals(1, second.dragCalls);
        assertEquals(1, second.releaseCalls);
    }

    @Test
    void unhandledNewSameButtonPressClearsTheStaleOwnerAndDragging() {
        TestWidget first = new TestWidget(0, true, true);
        TestWidget unhandled = new TestWidget(0, false, true);
        TestHost host = new TestHost(first);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        host.children = List.of(unhandled);
        assertFalse(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertFalse(host.dragging);
        assertFalse(router.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertFalse(router.mouseReleased(host, event(0, 5.0D, 5.0D)));
    }

    @Test
    void differentMouseButtonsKeepIndependentOwners() {
        TestWidget left = new TestWidget(0, true, true);
        TestWidget right = new TestWidget(1, true, true);
        TestHost host = new TestHost(left, right);
        ContainerWidgetPointerRouter router = new ContainerWidgetPointerRouter();

        assertTrue(router.mouseClicked(host, event(0, 5.0D, 5.0D), false));
        assertTrue(router.mouseClicked(host, event(1, 5.0D, 5.0D), false));
        assertTrue(host.dragging);
        assertTrue(router.mouseReleased(host, event(1, 5.0D, 5.0D)));
        assertTrue(host.dragging);
        assertEquals(1, right.releaseCalls);
        assertEquals(0, left.releaseCalls);
        assertTrue(router.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertTrue(router.mouseReleased(host, event(0, 5.0D, 5.0D)));
        assertEquals(1, left.dragCalls);
        assertEquals(1, left.releaseCalls);
    }

    @Test
    void routerInstancesDoNotSharePointerOwners() {
        TestWidget first = new TestWidget(0, true, true);
        TestWidget second = new TestWidget(0, true, true);
        TestHost firstHost = new TestHost(first);
        TestHost secondHost = new TestHost(second);
        ContainerWidgetPointerRouter firstRouter = new ContainerWidgetPointerRouter();
        ContainerWidgetPointerRouter secondRouter = new ContainerWidgetPointerRouter();

        assertTrue(firstRouter.mouseClicked(firstHost, event(0, 5.0D, 5.0D), false));
        assertFalse(secondRouter.mouseDragged(event(0, 5.0D, 5.0D), 1.0D, 1.0D));
        assertFalse(secondRouter.mouseReleased(secondHost, event(0, 5.0D, 5.0D)));
        assertTrue(firstRouter.mouseReleased(firstHost, event(0, 5.0D, 5.0D)));
        assertEquals(1, first.releaseCalls);
        assertEquals(0, second.releaseCalls);
    }

    private static MouseButtonEvent event(int button, double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }

    private static final class TestHost implements ContainerEventHandler {

        private List<? extends GuiEventListener> children;
        private GuiEventListener focused;
        private boolean dragging;

        private TestHost(GuiEventListener... children) {
            this.children = List.of(children);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        @Nullable
        public GuiEventListener getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener listener) {
            this.focused = listener;
        }

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

    }

    private static final class TestWidget extends AbstractWidget implements FancyMenuWidget {

        private final int handledButton;
        private final boolean clickHandled;
        private final boolean takeFocus;
        private boolean dragHandled = true;
        private boolean releaseHandled = true;
        private int clickCalls;
        private int dragCalls;
        private int releaseCalls;

        private TestWidget(int handledButton, boolean clickHandled, boolean takeFocus) {
            super(0, 0, 10, 10, Component.empty());
            this.handledButton = handledButton;
            this.clickHandled = clickHandled;
            this.takeFocus = takeFocus;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            this.clickCalls++;
            return this.clickHandled && event.button() == this.handledButton;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            this.dragCalls++;
            return this.dragHandled;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            this.releaseCalls++;
            return this.releaseHandled;
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return this.takeFocus;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
