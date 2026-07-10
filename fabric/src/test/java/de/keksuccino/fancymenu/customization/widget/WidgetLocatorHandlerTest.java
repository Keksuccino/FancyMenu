package de.keksuccino.fancymenu.customization.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetLocatorHandlerTest {

    @Test
    void inactiveButtonIsSuccessfulNoOp() {
        AtomicInteger invocationCount = new AtomicInteger();
        Button button = Button.builder(Component.literal("Inactive"), ignored -> invocationCount.incrementAndGet()).build();
        button.active = false;

        boolean invoked = WidgetLocatorHandler.invokeWidgetOnClick(meta(button));

        assertTrue(invoked);
        assertEquals(0, invocationCount.get());
    }

    @Test
    void invisibleButtonIsSuccessfulNoOp() {
        AtomicInteger invocationCount = new AtomicInteger();
        Button button = Button.builder(Component.literal("Invisible"), ignored -> invocationCount.incrementAndGet()).build();
        button.visible = false;

        boolean invoked = WidgetLocatorHandler.invokeWidgetOnClick(meta(button));

        assertTrue(invoked);
        assertEquals(0, invocationCount.get());
    }

    @Test
    void activeButtonIsInvokedExactlyOnce() {
        AtomicInteger invocationCount = new AtomicInteger();
        Button button = Button.builder(Component.literal("Active"), ignored -> invocationCount.incrementAndGet()).build();

        boolean invoked = WidgetLocatorHandler.invokeWidgetOnClick(meta(button));

        assertTrue(invoked);
        assertEquals(1, invocationCount.get());
    }

    @Test
    void activeNonButtonWidgetRetainsDirectClickSemantics() {
        CountingWidget widget = new CountingWidget();

        boolean invoked = WidgetLocatorHandler.invokeWidgetOnClick(meta(widget));

        assertTrue(invoked);
        assertEquals(1, widget.invocationCount);
    }

    @Test
    void invalidLocatorStillFails() {
        assertFalse(WidgetLocatorHandler.invokeWidgetOnClick("missing"));
    }

    @Test
    void callbackExceptionStillFails() {
        CountingWidget widget = new CountingWidget(true);

        boolean invoked = WidgetLocatorHandler.invokeWidgetOnClick(meta(widget));

        assertFalse(invoked);
        assertEquals(1, widget.invocationCount);
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta meta(AbstractWidget widget) {
        // Click invocation only reads the widget; null avoids constructing a Minecraft-dependent parent screen.
        return new WidgetMeta(widget, 1L, null);
    }

    private static class CountingWidget extends AbstractWidget {

        private final boolean throwOnClick;
        private int invocationCount;

        private CountingWidget() {
            this(false);
        }

        private CountingWidget(boolean throwOnClick) {
            super(0, 0, 150, 20, Component.literal("Counting"));
            this.throwOnClick = throwOnClick;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.invocationCount++;
            if (this.throwOnClick) throw new IllegalStateException("Expected test callback failure");
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
