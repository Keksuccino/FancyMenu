package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In 1.19.2, the widget that had the focus before loses it in Screen#setFocused through MixinAbstractContainerEventHandler,
 * which unit tests don't apply, so MixinAbstractContainerEventHandlerFocusTest covers that part.
 */
class InitialWidgetFocusScreenTest {

    @BeforeEach
    @AfterEach
    void clearPendingFocus() {
        // All screens share the pending focus, because it lives in the fields of the interface
        InitialWidgetFocusScreen.doInitialWidgetFocusAction.set(false);
        InitialWidgetFocusScreen.parentScreenOfInitialFocusWidget.set(null);
        InitialWidgetFocusScreen.initialFocusWidget.set(null);
    }

    @Test
    void theWidgetGetsFocusedOnTheFirstRender() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestWidget input = new TestWidget();

        screen.setupInitialFocusWidget(screen, input);
        assertFalse(input.isFocused());
        assertNull(screen.getFocused());

        screen.performInitialWidgetFocusActionInRender();
        assertSame(input, screen.getFocused());
        assertTrue(input.isFocused());
    }

    @Test
    void theFocusGetsSetOnlyOnceSoClicksCanMoveIt() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestWidget input = new TestWidget();
        TestWidget other = new TestWidget();
        screen.setupInitialFocusWidget(screen, input);
        screen.performInitialWidgetFocusActionInRender();

        click(screen, other);
        screen.performInitialWidgetFocusActionInRender();

        assertSame(other, screen.getFocused());
        assertTrue(other.isFocused());
        assertFalse(input.isFocused());
    }

    @Test
    void theWidgetOfTheLatestSetupGetsTheFocus() {
        // A resize runs init() again, which builds new widgets
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestWidget oldInput = new TestWidget();
        TestWidget newInput = new TestWidget();
        screen.setupInitialFocusWidget(screen, oldInput);
        screen.setupInitialFocusWidget(screen, newInput);

        screen.performInitialWidgetFocusActionInRender();

        assertSame(newInput, screen.getFocused());
        assertTrue(newInput.isFocused());
        assertFalse(oldInput.isFocused());
    }

    @Test
    void theFocusGoesToTheScreenOfTheWidgetWhenAnotherScreenRendersFirst() {
        TestScreen window = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestScreen otherWindow = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestWidget input = new TestWidget();
        window.setupInitialFocusWidget(window, input);

        otherWindow.performInitialWidgetFocusActionInRender();

        assertSame(input, window.getFocused());
        assertTrue(input.isFocused());
        assertNull(otherWindow.getFocused());
    }

    @Test
    void theFocusStaysWhereItWasWhenTheScreenRefusesIt() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestWidget other = new TestWidget();
        TestWidget input = new TestWidget();
        click(screen, other);
        screen.refuseFocus = true;

        screen.setupInitialFocusWidget(screen, input);
        screen.performInitialWidgetFocusActionInRender();

        assertSame(other, screen.getFocused());
        assertTrue(other.isFocused());
        assertFalse(input.isFocused());
    }

    /**
     * Moves the focus to the widget like a click on it does.
     */
    private static void click(@NotNull TestScreen screen, @NotNull TestWidget widget) {
        if (screen.getFocused() instanceof AbstractWidget focused) focused.setFocused(false);
        widget.setFocused(true);
        screen.setFocused(widget);
    }

    private static final class TestScreen extends Screen implements InitialWidgetFocusScreen {

        private boolean refuseFocus;

        private TestScreen() {
            super(Component.empty());
        }

        @Override
        public void setFocused(@Nullable GuiEventListener listener) {
            // Like FancyMenu's screens do for widgets that are not focusable
            if (!this.refuseFocus) super.setFocused(listener);
        }

    }

    private static final class TestWidget extends AbstractWidget {

        private TestWidget() {
            super(0, 0, 100, 20, Component.empty());
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
        }

    }

}
