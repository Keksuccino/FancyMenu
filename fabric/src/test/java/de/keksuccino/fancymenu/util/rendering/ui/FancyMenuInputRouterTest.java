package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FancyMenuInputRouterTest {

    @Test
    void scrollRoutingSkipsVanillaChildrenAndStopsAtFirstFancyMenuConsumer() {
        AtomicInteger vanillaCalls = new AtomicInteger();
        TestComponent first = new TestComponent(false);
        TestComponent consumer = new TestComponent(true);
        TestComponent afterConsumer = new TestComponent(true);
        GuiEventListener vanilla = new TestVanillaComponent(vanillaCalls);

        assertTrue(FancyMenuInputRouter.routeMouseScrolled(List.of(vanilla, first, consumer, afterConsumer), 10.0D, 20.0D, 1.0D, -2.0D));
        assertEquals(0, vanillaCalls.get());
        assertEquals(1, first.scrollCalls);
        assertEquals(1, consumer.scrollCalls);
        assertEquals(0, afterConsumer.scrollCalls);
    }

    @Test
    void unconsumedScrollVisitsEachFancyMenuComponentExactlyOnce() {
        TestComponent first = new TestComponent(false);
        TestComponent second = new TestComponent(false);

        assertFalse(FancyMenuInputRouter.routeMouseScrolled(List.of(first, second), 0.0D, 0.0D, 0.0D, 1.0D));
        assertEquals(1, first.scrollCalls);
        assertEquals(1, second.scrollCalls);
    }

    @Test
    void broadcastReleasePreservesLegacyFocusedComponentConsumption() {
        TestComponent component = new TestComponent(false);

        assertTrue(FancyMenuInputRouter.routeMouseReleased(List.of(component), component, 5.0D, 6.0D, 0, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS));
        assertEquals(1, component.releaseCalls);
        assertFalse(FancyMenuInputRouter.routeMouseReleased(List.of(), new TestVanillaComponent(new AtomicInteger()), 5.0D, 6.0D, 0, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS));
        assertFalse(FancyMenuInputRouter.routeMouseReleased(List.of(), null, 5.0D, 6.0D, 0, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS));
    }

    @Test
    void broadcastReleaseResolvesCaptureBeforeDispatchClearsIt() {
        CaptureAwareComponent component = new CaptureAwareComponent(1);
        TestVanillaComponent vanilla = new TestVanillaComponent(new AtomicInteger());

        assertTrue(FancyMenuInputRouter.routeMouseReleased(List.of(component, vanilla), vanilla, 5.0D, 6.0D, 1, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS));
        assertEquals(1, component.releaseCalls);
        assertFalse(component.hasMouseButtonCapture(1));
    }

    @Test
    void capturedOnlyReleaseDispatchesOnlyMatchingPointerOwners() {
        CaptureAwareComponent matching = new CaptureAwareComponent(2);
        CaptureAwareComponent other = new CaptureAwareComponent(1);
        TestComponent legacy = new TestComponent(false);

        assertTrue(FancyMenuInputRouter.routeMouseReleased(List.of(other, legacy, matching), null, 5.0D, 6.0D, 2, FancyMenuInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY));
        assertEquals(0, other.releaseCalls);
        assertEquals(0, legacy.releaseCalls);
        assertEquals(1, matching.releaseCalls);
        assertFalse(matching.hasMouseButtonCapture(2));
    }

    @Test
    void capturedOnlyReleaseReturnsFalseWithoutDispatchWhenButtonIsUnowned() {
        CaptureAwareComponent component = new CaptureAwareComponent(1);

        assertFalse(FancyMenuInputRouter.routeMouseReleased(List.of(component), component, 5.0D, 6.0D, 0, FancyMenuInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY));
        assertEquals(0, component.releaseCalls);
        assertTrue(component.hasMouseButtonCapture(1));
    }

    private static class TestComponent implements GuiEventListener, FancyMenuUiComponent {

        private final boolean consumeScroll;
        int scrollCalls;
        int releaseCalls;
        private boolean focused;

        private TestComponent(boolean consumeScroll) {
            this.consumeScroll = consumeScroll;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.scrollCalls++;
            return this.consumeScroll;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.releaseCalls++;
            return false;
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

    private static final class CaptureAwareComponent extends TestComponent implements MouseButtonCaptureOwner {

        private int capturedButton;

        private CaptureAwareComponent(int capturedButton) {
            super(false);
            this.capturedButton = capturedButton;
        }

        @Override
        public boolean hasMouseButtonCapture(int button) {
            return button == this.capturedButton;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            super.mouseReleased(mouseX, mouseY, button);
            if (button == this.capturedButton) this.capturedButton = -1;
            return true;
        }
    }

    private static final class TestVanillaComponent implements GuiEventListener {

        private final AtomicInteger scrollCalls;
        private boolean focused;

        private TestVanillaComponent(AtomicInteger scrollCalls) {
            this.scrollCalls = scrollCalls;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.scrollCalls.incrementAndGet();
            return true;
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

}
