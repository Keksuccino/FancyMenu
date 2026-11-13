package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererInteractionTest {

    private static final String TEXT = "plain words [two words](https://example.invalid) more\n\n[click me](click:click_id) and [hover me](hover:hover_id)\n\n| A | B |\n| --- | --- |\n| plain | cell |\n\n| A | B |\n| --- | --- |\n| [cell click](click:cell_id) | cell |\n\nlast line";

    @Test
    void childrenAreTheFragmentsThatReactToInput() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        renderer.tick();

        List<MarkdownTextFragment> expected = new ArrayList<>();
        for (MarkdownTextFragment fragment : renderer.fragments) {
            if ((fragment.hyperlink != null) || (fragment.clickEvent != null) || (fragment.hoverEvent != null)) expected.add(fragment);
        }
        expected.add(renderer.tableFragment(1));

        assertEquals(7, expected.size());
        assertEquals(expected, renderer.children());
        assertFalse(renderer.children().contains(renderer.tableFragment(0)));
        assertFalse(renderer.children().contains(renderer.fragmentContaining("plain")));
    }

    @Test
    void childrenFollowANewText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        renderer.tick();

        renderer.setText("only plain words");
        renderer.tick();
        assertTrue(renderer.children().isEmpty());

        renderer.setText("a [link](https://example.invalid)");
        renderer.tick();
        assertEquals(List.of(renderer.fragmentContaining("link")), renderer.children());
    }

    @Test
    void anyTextUnderTheMouseGetsFoundButOnlyInteractiveTextHandlesClicks() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment plain = renderer.fragmentContaining("plain");
        MarkdownTextFragment last = renderer.fragmentContaining("last");

        assertSame(plain, renderer.getChildAt(plain.getTextX() + 1, plain.getTextY() + 1).orElse(null));
        assertSame(last, renderer.getChildAt(last.getTextX() + 1, last.getTextY() + 1).orElse(null));
        // Right of the text of the last line, and outside of the renderer
        assertTrue(renderer.getChildAt(last.getTextX() + last.getTextWidth() + 50, last.getTextY() + 1).isEmpty());
        assertTrue(renderer.getChildAt(0, 0).isEmpty());
        // In this Minecraft version, a click on a container only counts as handled when one of its children handles it
        assertFalse(renderer.mouseClicked(plain.getTextX() + 1, plain.getTextY() + 1, 0));
        assertFalse(renderer.mouseClicked(0, 0, 0));
    }

    @Test
    void clickFiresTheClickEventOfTheHoveredFragment() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment click = renderer.fragmentContaining("click");

        // Rendering sets the hover state of a fragment
        click.setHovered(true);
        assertTrue(renderer.mouseClicked(click.getTextX() + 1, click.getTextY() + 1, 0));

        assertEquals(List.of("click_id"), events.clicked);
    }

    @Test
    void clickReachesTheFragmentsInsideATable() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment table = renderer.tableFragment(1);
        MarkdownTextFragment cellClick = MarkdownTestRenderer.fragmentContaining(table.tableContext.rows.get(1).cells.get(0).fragments, "cell");

        // Rendering a table positions the fragments of its cells and sets their hover state
        cellClick.x = table.x + renderer.getTableCellPadding();
        cellClick.y = table.y + renderer.getTableMargin() + table.tableContext.rows.get(0).height + renderer.getTableCellPadding();
        cellClick.setHovered(true);
        assertTrue(renderer.mouseClicked(cellClick.getTextX() + 1, cellClick.getTextY() + 1, 0));

        assertEquals(List.of("cell_id"), events.clicked);
    }

    @Test
    void linesOutsideTheVisibleRangeDoNotGetFoundUnderTheMouse() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        renderer.tick();
        MarkdownTextFragment plain = renderer.fragmentContaining("plain");
        MarkdownTextFragment last = renderer.fragmentContaining("last");

        renderer.setVisibleVerticalRange(last.y - 1, last.y + 100);

        assertTrue(renderer.getChildAt(plain.getTextX() + 1, plain.getTextY() + 1).isEmpty());
        assertSame(last, renderer.getChildAt(last.getTextX() + 1, last.getTextY() + 1).orElse(null));
    }

    @Test
    void hoverEventFiresOnceEveryTimeTheMouseEntersIt() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[first second](hover:one) and [third](hover:two)", 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");
        MarkdownTextFragment third = renderer.fragmentContaining("third");

        renderer.updateTextHoverEvents();
        assertTrue(events.hovered.isEmpty());

        first.setHovered(true);
        renderer.updateTextHoverEvents();
        renderer.updateTextHoverEvents();
        assertEquals(List.of("one"), events.hovered);

        // Moving on to another fragment of the same event is not a new hover
        first.setHovered(false);
        second.setHovered(true);
        renderer.updateTextHoverEvents();
        assertEquals(List.of("one"), events.hovered);

        second.setHovered(false);
        third.setHovered(true);
        renderer.updateTextHoverEvents();
        assertEquals(List.of("one", "two"), events.hovered);

        third.setHovered(false);
        renderer.updateTextHoverEvents();
        first.setHovered(true);
        renderer.updateTextHoverEvents();
        assertEquals(List.of("one", "two", "one"), events.hovered);
    }

    @Test
    void hoverEventsWithTheSameIdentifierCountAsOne() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[first](hover:same) and [second](hover:same)", 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");

        first.setHovered(true);
        renderer.updateTextHoverEvents();
        first.setHovered(false);
        second.setHovered(true);
        renderer.updateTextHoverEvents();

        assertEquals(List.of("same"), events.hovered);
    }

    @Test
    void hoverEventsInsideTablesFire() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("| A |\n| --- |\n| [cell](hover:cell_hover) |", 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment cell = MarkdownTestRenderer.fragmentContaining(renderer.tableFragment(0).tableContext.rows.get(1).cells.get(0).fragments, "cell");

        cell.setHovered(true);
        renderer.updateTextHoverEvents();

        assertEquals(List.of("cell_hover"), events.hovered);
        assertEquals(List.of(renderer.tableFragment(0)), renderer.children());
    }

    @Test
    void resetHoveredMakesEveryFragmentForgetTheMouse() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 600);
        RecordingEventHandler events = new RecordingEventHandler();
        renderer.setTextEventHandler(events);
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("two");
        MarkdownTextFragment hover = renderer.fragmentContaining("hover");
        MarkdownTextFragment cellClick = MarkdownTestRenderer.fragmentContaining(renderer.tableFragment(1).tableContext.rows.get(1).cells.get(0).fragments, "cell");
        link.setHovered(true);
        hover.setHovered(true);
        cellClick.setHovered(true);
        renderer.updateTextHoverEvents();

        renderer.resetHovered();

        assertFalse(link.hovered);
        assertFalse(link.hyperlink.isHovered());
        assertFalse(hover.hoverEvent.isHovered());
        assertFalse(cellClick.hovered);
        assertFalse(cellClick.clickEvent.isHovered());

        // The mouse entering the event again is a new hover
        hover.setHovered(true);
        renderer.updateTextHoverEvents();
        assertEquals(List.of("hover_id", "hover_id"), events.hovered);
    }

    private static final class RecordingEventHandler implements MarkdownRenderer.TextEventHandler {

        private final List<String> clicked = new ArrayList<>();
        private final List<String> hovered = new ArrayList<>();

        @Override
        public void onTextClickEvent(@NotNull String eventId) {
            this.clicked.add(eventId);
        }

        @Override
        public void onTextHoverEvent(@NotNull String eventId) {
            this.hovered.add(eventId);
        }

    }

}
