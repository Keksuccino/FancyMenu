package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownStrikethroughTest {

    private static final int CHAR_WIDTH = 6;

    @Test
    void lineThroughStruckTextEndsBehindItsLastWord() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ~~struck~~ b", 400);
        renderer.tick();
        MarkdownTextFragment struck = renderer.fragmentContaining("struck");

        // The fragment keeps the space behind its word, which still gets drawn
        assertEquals("struck ", struck.text);
        assertEquals("struck ", struck.getRenderComponent().getString());
        assertEquals("struck", struckThroughText(struck.getRenderComponent()));
    }

    @Test
    void lineThroughStruckTextRunsThroughTheSpacesBetweenItsWords() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("~~first second~~ b", 400);
        renderer.tick();

        assertEquals("first ", struckThroughText(renderer.fragmentContaining("first").getRenderComponent()));
        assertEquals("second", struckThroughText(renderer.fragmentContaining("second").getRenderComponent()));
    }

    @Test
    void lineThroughStruckTextEndsBehindTheLastWordOfALineTheTextWrapsAt() {
        // The line has room for "a first" and the space behind it, but not for the second struck through word
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ~~first second~~ b", 80);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertTrue(first.autoLineBreakAfter);
        assertNotSame(first.parentLine, renderer.fragmentContaining("second").parentLine);
        assertEquals("first", first.getRenderComponent().getString());
    }

    @Test
    void lineThroughStruckTextInATableCellEndsBehindItsLastWord() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("| Head |\n| --- |\n| ~~old~~ new |", 400);
        renderer.tick();
        MarkdownTextFragment old = MarkdownTestRenderer.fragmentContaining(renderer.tableFragment(0).tableContext.rows.get(1).cells.get(0).fragments, "old");

        assertEquals("old ", old.getRenderComponent().getString());
        assertEquals("old", struckThroughText(old.getRenderComponent()));
    }

    @Test
    void spaceBehindStruckTextKeepsItsWidth() {
        MarkdownTestRenderer plain = new MarkdownTestRenderer("a struck b", 400);
        MarkdownTestRenderer struck = new MarkdownTestRenderer("a ~~struck~~ b", 400);
        plain.tick();
        struck.tick();

        assertEquals(plain.fragmentContaining("struck").unscaledTextWidth, struck.fragmentContaining("struck").unscaledTextWidth);
        assertEquals(plain.fragmentContaining("b").x, struck.fragmentContaining("b").x);
    }

    @Test
    void fontDrawsTheLineUnlessTheStyleKeepsItOnThePixelGrid() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ~~struck~~ b", 400);
        renderer.setParentRenderScale(3.0F);
        renderer.tick();
        MarkdownTextFragment struck = renderer.fragmentContaining("struck");

        assertFalse(MarkdownStrikethroughPainter.drawsLine(struck));
        assertEquals("struck", struckThroughText(struck.getRenderComponent()));

        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();
        struck = renderer.fragmentContaining("struck");

        assertTrue(MarkdownStrikethroughPainter.drawsLine(struck));
        assertEquals("", struckThroughText(struck.getRenderComponent()));
        assertEquals("struck ", struck.getRenderComponent().getString());
        assertFalse(MarkdownStrikethroughPainter.drawsLine(renderer.fragmentContaining("b")));

        // Changing the option alone reaches the text without a new parse
        int parses = renderer.parses;
        renderer.getStyle().snapStrikethrough.set(false);
        renderer.tick();

        assertEquals(parses, renderer.parses);
        assertFalse(MarkdownStrikethroughPainter.drawsLine(struck));
        assertEquals("struck", struckThroughText(struck.getRenderComponent()));
    }

    @Test
    void lineOnThePixelGridIsAsWideAsTheStruckText() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("~~first second~~ b", 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();

        // The line runs through the space between two struck through words, and ends behind the last one
        assertEquals(6 * CHAR_WIDTH, MarkdownStrikethroughPainter.getLineWidth(renderer.fragmentContaining("first")));
        assertEquals(6 * CHAR_WIDTH, MarkdownStrikethroughPainter.getLineWidth(renderer.fragmentContaining("second")));

        renderer.setTextBaseScale(2.0F);
        renderer.tick();

        assertEquals(6 * CHAR_WIDTH * 2, MarkdownStrikethroughPainter.getLineWidth(renderer.fragmentContaining("second")));
    }

    @Test
    void lineOnThePixelGridEndsBehindTheLastWordOfALineTheTextWrapsAt() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ~~first second~~ b", 80);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertTrue(first.autoLineBreakAfter);
        assertEquals(5 * CHAR_WIDTH, MarkdownStrikethroughPainter.getLineWidth(first));
    }

    /**
     * Returns the parts of the component that have a line through them.
     */
    private static String struckThroughText(Component component) {
        StringBuilder struck = new StringBuilder();
        component.visit((style, text) -> {
            if (style.isStrikethrough()) struck.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return struck.toString();
    }

}
