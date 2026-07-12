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
    void callbackExceptionStillFails() {
        Button button = Button.builder(Component.literal("Throwing"), ignored -> {
            throw new IllegalStateException("Expected test failure");
        }).build();

        assertFalse(WidgetLocatorHandler.invokeWidgetOnClick(meta(button)));
    }

    @Test
    void invalidLocatorStillFails() {
        assertFalse(WidgetLocatorHandler.invokeWidgetOnClick("missing"));
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta meta(AbstractWidget widget) {
        // Click invocation only reads the widget; null avoids constructing a Minecraft-dependent parent screen.
        return new WidgetMeta(widget, 1L, null);
    }

    private static class CountingWidget extends AbstractWidget {

        private int invocationCount;

        private CountingWidget() {
            super(0, 0, 150, 20, Component.literal("Counting"));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.invocationCount++;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
