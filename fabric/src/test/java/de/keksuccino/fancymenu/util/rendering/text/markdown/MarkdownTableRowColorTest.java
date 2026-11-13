package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class MarkdownTableRowColorTest {

    private static final String TABLE = "| Name | Value |\n| --- | --- |\n| a | 1 |\n| b | 2 |\n| c | 3 |";
    private static final DrawableColor HEADER = DrawableColor.of(1, 2, 3);
    private static final DrawableColor ROW = DrawableColor.of(4, 5, 6);
    private static final DrawableColor ALTERNATE = DrawableColor.of(7, 8, 9);

    @Test
    void shownHeaderHasItsOwnColorAndTheRowsBehindItAlternate() {
        MarkdownTestRenderer renderer = renderer();
        MarkdownTextFragment.TableContext table = renderer.tableFragment(0).tableContext;

        assertSame(HEADER, table.getRowBackgroundColor(renderer, 0));
        assertSame(ROW, table.getRowBackgroundColor(renderer, 1));
        assertSame(ALTERNATE, table.getRowBackgroundColor(renderer, 2));
        assertSame(ROW, table.getRowBackgroundColor(renderer, 3));
    }

    @Test
    void headerRowIsARowLikeEveryOtherOneWhileHeadersAreNotShown() {
        MarkdownTestRenderer renderer = renderer();
        renderer.setTableShowHeader(false);
        MarkdownTextFragment.TableContext table = renderer.tableFragment(0).tableContext;

        assertSame(ROW, table.getRowBackgroundColor(renderer, 0));
        assertSame(ALTERNATE, table.getRowBackgroundColor(renderer, 1));
        assertSame(ROW, table.getRowBackgroundColor(renderer, 2));
        assertSame(ALTERNATE, table.getRowBackgroundColor(renderer, 3));
    }

    @Test
    void rowsShareOneColorWhileTheyDoNotAlternate() {
        MarkdownTestRenderer renderer = renderer();
        renderer.setTableAlternateRowColors(false);
        MarkdownTextFragment.TableContext table = renderer.tableFragment(0).tableContext;

        assertSame(HEADER, table.getRowBackgroundColor(renderer, 0));
        for (int row = 1; row < table.rows.size(); row++) {
            assertSame(ROW, table.getRowBackgroundColor(renderer, row));
        }
    }

    private static MarkdownTestRenderer renderer() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TABLE, 400);
        renderer.setTableHeaderBackgroundColor(HEADER);
        renderer.setTableRowBackgroundColor(ROW);
        renderer.setTableAlternateRowColor(ALTERNATE);
        renderer.tick();
        return renderer;
    }

}
