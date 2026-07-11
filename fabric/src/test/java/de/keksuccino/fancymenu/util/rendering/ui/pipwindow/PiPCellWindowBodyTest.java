package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiPCellWindowBodyTest {

    @Test
    void routesPastFirstHoveredUnhandledChildInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        RecordingListener row = new RecordingListener("row", true, false, calls);
        RecordingListener scrollArea = new RecordingListener("scrollArea", false, true, calls);
        TestPiPCellWindowBody screen = createScreen(row, scrollArea);

        assertTrue(screen.mouseScrolled(10.0D, 20.0D, 1.5D, -2.5D));
        assertEquals(List.of("row", "scrollArea"), calls);
        assertEquals(List.of(10.0D, 20.0D, 1.5D, -2.5D), scrollArea.lastEvent);
    }

    @Test
    void stopsRoutingAfterFirstChildHandlesEvent() {
        List<String> calls = new ArrayList<>();
        RecordingListener first = new RecordingListener("first", false, false, calls);
        RecordingListener handler = new RecordingListener("handler", false, true, calls);
        RecordingListener skipped = new RecordingListener("skipped", false, true, calls);
        TestPiPCellWindowBody screen = createScreen(first, handler, skipped);

        assertTrue(screen.mouseScrolled(1.0D, 2.0D, 3.0D, 4.0D));
        assertEquals(List.of("first", "handler"), calls);
    }

    @Test
    void returnsFalseAfterEveryChildDeclinesEvent() {
        List<String> calls = new ArrayList<>();
        RecordingListener first = new RecordingListener("first", true, false, calls);
        RecordingListener second = new RecordingListener("second", false, false, calls);
        TestPiPCellWindowBody screen = createScreen(first, second);

        assertFalse(screen.mouseScrolled(1.0D, 2.0D, 3.0D, 4.0D));
        assertEquals(List.of("first", "second"), calls);
    }

    private static TestPiPCellWindowBody createScreen(GuiEventListener... listeners) {
        TestPiPCellWindowBody screen = ScreenTestFactory.allocateScreen(TestPiPCellWindowBody.class);
        screen.listeners = List.of(listeners);
        return screen;
    }

    private static final class TestPiPCellWindowBody extends PiPCellWindowBody {

        private List<? extends GuiEventListener> listeners;

        private TestPiPCellWindowBody() {
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.listeners;
        }

        @Override
        protected void onCancel() {
        }

        @Override
        protected void onDone() {
        }

    }

    private static final class RecordingListener implements GuiEventListener {

        private final String name;
        private final boolean hovered;
        private final boolean handlesScroll;
        private final List<String> calls;
        private List<Double> lastEvent;

        private RecordingListener(String name, boolean hovered, boolean handlesScroll, List<String> calls) {
            this.name = name;
            this.hovered = hovered;
            this.handlesScroll = handlesScroll;
            this.calls = calls;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            this.calls.add(this.name);
            this.lastEvent = List.of(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            return this.handlesScroll;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.hovered;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

}
