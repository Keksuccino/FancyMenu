package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CellScreenReselectionTest {

    private final TestScreen screen = ScreenTestFactory.allocateScreen(TestScreen.class);

    @Test
    void theCellWithTheSameIdentityTakesOverTheSelectionWhereverItIsNow() {
        // Like after adding a listener, which sorts the list anew
        CellScreen.RenderCell selected = this.cell("b");
        CellScreen.CellSelection selection = selectionOf(selected, List.of(this.cell("a"), selected, this.cell("c")));
        CellScreen.RenderCell rebuilt = this.cell("b");

        assertSame(rebuilt, CellScreen.findCellToReselect(selection, List.of(this.cell("new"), this.cell("a"), this.cell("c"), rebuilt)));
    }

    @Test
    void noCellTakesOverTheSelectionWhenTheCellWithItsIdentityIsGone() {
        // Like after removing the selected listener, where another listener moves up to its position
        CellScreen.RenderCell selected = this.cell("b");
        CellScreen.CellSelection selection = selectionOf(selected, List.of(this.cell("a"), selected, this.cell("c")));

        assertNull(CellScreen.findCellToReselect(selection, List.of(this.cell("a"), this.cell("c"), this.cell("d"))));
    }

    @Test
    void cellsWithoutIdentityHandTheSelectionToTheCellAtTheirPosition() {
        // Like after resizing the window, which builds the same cells again
        CellScreen.RenderCell selected = this.cell(null);
        CellScreen.CellSelection selection = selectionOf(selected, List.of(this.cell(null), selected, this.cell(null)));
        CellScreen.RenderCell rebuilt = this.cell(null);

        assertSame(rebuilt, CellScreen.findCellToReselect(selection, List.of(this.cell(null), rebuilt, this.cell(null))));
    }

    @Test
    void cellsWithoutIdentityKeepNoSelectionWhenCellsGotAddedOrRemoved() {
        CellScreen.RenderCell selected = this.cell(null);
        CellScreen.CellSelection selection = selectionOf(selected, List.of(this.cell(null), selected, this.cell(null)));

        assertNull(CellScreen.findCellToReselect(selection, List.of(this.cell(null), this.cell(null))));
        assertNull(CellScreen.findCellToReselect(selection, List.of(this.cell(null), this.cell(null), this.cell(null), this.cell(null))));
    }

    @Test
    void cellsWithoutIdentityKeepNoSelectionWhenAnotherTypeOfCellIsAtTheirPosition() {
        CellScreen.RenderCell selected = this.cell(null);
        CellScreen.CellSelection selection = selectionOf(selected, List.of(this.cell(null), selected));

        assertNull(CellScreen.findCellToReselect(selection, List.of(this.cell(null), this.screen.new OtherTestCell())));
        // A cell with an identity is another cell than the one that had none
        assertNull(CellScreen.findCellToReselect(selection, List.of(this.cell(null), this.cell("b"))));
    }

    @Test
    void cellsThatCanNotGetSelectedTakeOverNoSelection() {
        CellScreen.RenderCell selected = this.cell("b");
        CellScreen.CellSelection selection = selectionOf(selected, List.of(selected));
        CellScreen.RenderCell rebuilt = this.cell("b");
        rebuilt.setSelectable(false);

        assertNull(CellScreen.findCellToReselect(selection, List.of(rebuilt)));
    }

    @NotNull
    private CellScreen.RenderCell cell(@Nullable Object identity) {
        return this.screen.new TestCell().setIdentity(identity);
    }

    @NotNull
    private static CellScreen.CellSelection selectionOf(@NotNull CellScreen.RenderCell selected, @NotNull List<CellScreen.RenderCell> cells) {
        return new CellScreen.CellSelection(selected.getIdentity(), cells.indexOf(selected), cells.size(), selected.getClass());
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

        private final class OtherTestCell extends RenderCell {

            @Override
            public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            }

        }

    }

}
