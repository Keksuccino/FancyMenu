package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownUnderlineTest {

    @Test
    void underlineEndsBehindTheLastWord() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ++under++ b", 400);
        renderer.tick();
        MarkdownTextFragment under = renderer.fragmentContaining("under");

        // The fragment keeps the space behind its word, which still gets drawn
        assertEquals("under ", under.text);
        assertTrue(under.underlineEnd);
        assertEquals("under ", under.getRenderComponent().getString());
        assertEquals("under", underlinedText(under.getRenderComponent()));
        assertEquals("", underlinedText(renderer.fragmentContaining("b").getRenderComponent()));
    }

    @Test
    void underlineRunsThroughTheSpacesBetweenItsWords() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("++first second++ b", 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertFalse(first.underlineEnd);
        assertEquals("first ", underlinedText(first.getRenderComponent()));
        assertEquals("second", underlinedText(renderer.fragmentContaining("second").getRenderComponent()));
    }

    @Test
    void underlineEndsBehindTheLastWordOfALineTheTextWrapsAt() {
        // The line has room for "a first" and the space behind it, but not for the second underlined word
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a ++first second++ b", 80);
        renderer.setAutoLineBreakingEnabled(true);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");

        assertTrue(first.autoLineBreakAfter);
        assertNotSame(first.parentLine, renderer.fragmentContaining("second").parentLine);
        assertEquals("first", first.getRenderComponent().getString());
        assertEquals("first", underlinedText(first.getRenderComponent()));
    }

    @Test
    void underlineInATableCellEndsBehindItsLastWord() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("| Head |\n| --- |\n| ++old++ new |", 400);
        renderer.tick();
        MarkdownTextFragment old = MarkdownTestRenderer.fragmentContaining(renderer.tableFragment(0).tableContext.rows.get(1).cells.get(0).fragments, "old");

        assertEquals("old ", old.getRenderComponent().getString());
        assertEquals("old", underlinedText(old.getRenderComponent()));
    }

    @Test
    void underlinedTextKeepsTheWidthOfThePlainText() {
        MarkdownTestRenderer plain = new MarkdownTestRenderer("a under b", 400);
        MarkdownTestRenderer underlined = new MarkdownTestRenderer("a ++under++ b", 400);
        plain.tick();
        underlined.tick();

        assertEquals(plain.fragmentContaining("under").unscaledTextWidth, underlined.fragmentContaining("under").unscaledTextWidth);
        assertEquals(plain.fragmentContaining("b").x, underlined.fragmentContaining("b").x);
    }

    @Test
    void struckThroughAndUnderlinedTextEachEndWhereTheirOwnTextEnds() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("++~~struck~~ under++ b", 400);
        renderer.tick();
        MarkdownTextFragment struck = renderer.fragmentContaining("struck");

        assertTrue(struck.strikethroughEnd);
        assertFalse(struck.underlineEnd);
        assertTrue(renderer.fragmentContaining("under").underlineEnd);
    }

    /**
     * Returns the parts of the component that have a line below them.
     */
    private static String underlinedText(Component component) {
        StringBuilder underlined = new StringBuilder();
        component.visit((style, text) -> {
            if (style.isUnderlined()) underlined.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return underlined.toString();
    }

}
