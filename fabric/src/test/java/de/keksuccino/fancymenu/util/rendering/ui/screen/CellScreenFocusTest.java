package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellScreenFocusTest {

    @Test
    void focusingAWidgetOfACellTellsTheWidgetAndFocusesTheCell() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestScreen.TestCell cell = screen.new TestCell();
        TestWidget input = new TestWidget();

        screen.focusTarget(cell, input);

        assertTrue(input.isFocused());
        assertSame(input, cell.getFocused());
        assertSame(cell, screen.getFocused());
    }

    @Test
    void focusingAnotherWidgetOfTheCellTakesTheFocusFromThePreviousOne() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestScreen.TestCell cell = screen.new TestCell();
        TestWidget input = new TestWidget();
        TestWidget button = new TestWidget();

        screen.focusTarget(cell, input);
        screen.focusTarget(cell, button);

        assertFalse(input.isFocused());
        assertTrue(button.isFocused());
        assertSame(button, cell.getFocused());
    }

    @Test
    void upAndDownMoveAwayFromTheSelectedCell() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestScreen.TestCell selected = screen.new TestCell();
        TestScreen.TestCell focused = screen.new TestCell();
        screen.selectedCell = selected;
        screen.setFocused(focused);

        assertSame(selected, screen.getVerticalNavigationOriginCell());
    }

    @Test
    void upAndDownMoveAwayFromTheFocusedCellWhenNoCellIsSelected() {
        // Like the text input that takes the focus when a value editor opens
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        TestScreen.TestCell focused = screen.new TestCell();
        screen.focusTarget(focused, new TestWidget());

        assertSame(focused, screen.getVerticalNavigationOriginCell());
    }

    @Test
    void upAndDownHaveNoCellToMoveAwayFromWhenNeitherACellIsSelectedNorFocused() {
        TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);
        assertNull(screen.getVerticalNavigationOriginCell());

        // Like the search bar, which is no cell
        screen.setFocused(new TestWidget());
        assertNull(screen.getVerticalNavigationOriginCell());
    }

    private static final class TestScreen extends CellScreen {

        private TestScreen() {
            super(Component.empty());
        }

        @Override
        protected void onCancel() {
        }

        @Override
        protected void onDone() {
        }

        private final class TestCell extends RenderCell {

            @Override
            public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            }

        }

    }

    private static final class TestWidget extends AbstractWidget {

        private TestWidget() {
            super(0, 0, 100, 20, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
