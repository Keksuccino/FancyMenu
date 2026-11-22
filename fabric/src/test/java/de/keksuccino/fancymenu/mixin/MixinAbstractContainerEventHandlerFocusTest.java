package de.keksuccino.fancymenu.mixin;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEditBox;
import de.keksuccino.fancymenu.mixin.mixins.common.client.MixinAbstractContainerEventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class MixinAbstractContainerEventHandlerFocusTest {

    @Test
    void theFieldThatHadTheFocusLosesItWhenAnotherOneGetsIt() throws Exception {
        TestHandler handler = new TestHandler();
        TestWidget first = new TestWidget();
        TestWidget second = new TestWidget();
        click(handler, first);

        click(handler, second);

        assertSame(second, handler.focused);
        assertTrue(second.isFocused());
        assertFalse(first.isFocused());
    }

    @Test
    void focusingTheFocusedFieldAgainKeepsItFocused() throws Exception {
        TestHandler handler = new TestHandler();
        TestWidget field = new TestWidget();
        click(handler, field);

        click(handler, field);

        assertSame(field, handler.focused);
        assertTrue(field.isFocused());
    }

    @Test
    void clearingTheFocusUnfocusesTheField() throws Exception {
        TestHandler handler = new TestHandler();
        TestWidget field = new TestWidget();
        click(handler, field);

        setFocused(handler, null);

        assertNull(handler.focused);
        assertFalse(field.isFocused());
    }

    @Test
    void theNewListenerDoesNotGetFocusedSoChangeFocusCanToggleItOn() throws Exception {
        // setInitialFocus and Tab navigation of 1.19.2 set the focused listener and toggle its focus with changeFocus
        TestHandler handler = new TestHandler();
        TestWidget other = new TestWidget();
        TestWidget field = new TestWidget();
        click(handler, other);

        setFocused(handler, field);
        assertFalse(field.isFocused());
        assertTrue(field.changeFocus(true));

        assertTrue(field.isFocused());
        assertFalse(other.isFocused());
    }

    @Test
    void fieldsThatCannotLoseTheirFocusKeepIt() throws Exception {
        TestHandler handler = new TestHandler();
        AbstractWidget chatInput = mock(AbstractWidget.class, withSettings().extraInterfaces(IMixinEditBox.class));
        when(((IMixinEditBox) chatInput).getCanLoseFocusFancyMenu()).thenReturn(false);
        handler.focused = chatInput;

        click(handler, new TestWidget());

        verify(chatInput, never()).setFocused(anyBoolean());
    }

    @Test
    void fieldsThatCanLoseTheirFocusLoseIt() throws Exception {
        TestHandler handler = new TestHandler();
        AbstractWidget searchBox = mock(AbstractWidget.class, withSettings().extraInterfaces(IMixinEditBox.class));
        when(((IMixinEditBox) searchBox).getCanLoseFocusFancyMenu()).thenReturn(true);
        handler.focused = searchBox;

        click(handler, new TestWidget());

        verify(searchBox).setFocused(false);
    }

    @Test
    void unfocusableWidgetsNeitherGetTheFocusNorTakeIt() throws Exception {
        TestHandler handler = new TestHandler();
        TestWidget field = new TestWidget();
        UnfocusableWidget unfocusable = new UnfocusableWidget();
        click(handler, field);

        assertTrue(setFocused(handler, unfocusable).isCancelled());

        assertSame(field, handler.focused);
        assertTrue(field.isFocused());
    }

    /**
     * Focuses the widget like a click on an edit box does in 1.19.2: the edit box focuses itself, then its parent makes it the focused listener.
     */
    private static void click(@NotNull TestHandler handler, @NotNull AbstractWidget widget) throws Exception {
        widget.setFocused(true);
        setFocused(handler, widget);
    }

    /**
     * Runs the production hook like its injection at the head of AbstractContainerEventHandler#setFocused does, because a unit-test JVM does not apply mixins.
     */
    @NotNull
    private static CallbackInfo setFocused(@NotNull TestHandler handler, @Nullable GuiEventListener listener) throws Exception {
        Method hook = MixinAbstractContainerEventHandler.class.getDeclaredMethod("beforeSetFocusedFancyMenu", GuiEventListener.class, CallbackInfo.class);
        hook.setAccessible(true);
        CallbackInfo info = new CallbackInfo("setFocused", true);
        hook.invoke(handler, listener, info);
        if (!info.isCancelled()) handler.focused = listener;
        return info;
    }

    private static final class TestHandler extends MixinAbstractContainerEventHandler {

        @Nullable
        private GuiEventListener focused;
        private boolean dragging;

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        public @Nullable GuiEventListener getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            this.focused = focused;
        }

    }

    private static class TestWidget extends AbstractWidget {

        private TestWidget() {
            super(0, 0, 100, 20, Component.empty());
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
        }

    }

    private static final class UnfocusableWidget extends TestWidget implements NavigatableWidget {

        @Override
        public boolean isFocusable() {
            return false;
        }

        @Override
        public void setFocusable(boolean focusable) {
        }

        @Override
        public boolean isNavigatable() {
            return false;
        }

        @Override
        public void setNavigatable(boolean navigatable) {
        }

    }

}
