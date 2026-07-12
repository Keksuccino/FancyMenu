package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputFieldWidgetVisibilityControllerTest {

    @Test
    void initiallyHiddenWidgetIsRemovedFromHitTesting() {
        TestWidget widget = new TestWidget();

        InputFieldWidgetVisibilityController.synchronize(widget, false, null);

        assertFalse(widget.visible);
        assertFalse(widget.isMouseOver(5.0D, 5.0D));
        assertFalse(widget.isFocused());
    }

    @Test
    void hidingWidgetClearsParentOwnedFocus() {
        TestWidget widget = new TestWidget();
        TestContainer parent = new TestContainer(List.of(widget));
        parent.setFocused(widget);

        InputFieldWidgetVisibilityController.synchronize(widget, false, parent);

        assertNull(parent.getFocused());
        assertFalse(widget.isFocused());
        assertFalse(widget.visible);
    }

    @Test
    void hidingWidgetDoesNotClearAnotherParentsFocusedChild() {
        TestWidget widget = new TestWidget();
        TestWidget otherWidget = new TestWidget();
        TestContainer parent = new TestContainer(List.of(widget, otherWidget));
        parent.setFocused(otherWidget);
        widget.setFocused(true);

        InputFieldWidgetVisibilityController.synchronize(widget, false, parent);

        assertSame(otherWidget, parent.getFocused());
        assertTrue(otherWidget.isFocused());
        assertFalse(widget.isFocused());
    }

    @Test
    void showingWidgetRestoresHitTestingWithoutClaimingFocus() {
        TestWidget widget = new TestWidget();
        InputFieldWidgetVisibilityController.synchronize(widget, false, null);

        InputFieldWidgetVisibilityController.synchronize(widget, true, null);

        assertTrue(widget.visible);
        assertTrue(widget.isMouseOver(5.0D, 5.0D));
        assertFalse(widget.isFocused());
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

    private static final class TestWidget extends AbstractWidget {

        private TestWidget() {
            super(0, 0, 10, 10, Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    }

}
