package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.tableFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The comments show the Markdown source without the escaping of the Java strings.
 */
class MarkdownParserImageAltTextTest {

    @Test
    void imageFragmentHoldsItsAlternativeText() {
        List<MarkdownTextFragment> fragments = parse("![The alternative text](image.png)\ntext");

        assertNotNull(fragments.get(0).imageSupplier);
        assertEquals("The alternative text", fragments.get(0).imageAltText);
        assertNull(fragments.get(1).imageAltText);
    }

    @Test
    void hyperlinkImageFragmentHoldsTheAlternativeTextOfItsImage() {
        List<MarkdownTextFragment> fragments = parse("[![Logo (dark)](logo.png)](https://example.invalid)\ntext");

        assertNotNull(fragments.get(0).imageSupplier);
        assertEquals("Logo (dark)", fragments.get(0).imageAltText);
        assertEquals("https://example.invalid", fragments.get(0).hyperlink.link);
        assertNull(fragments.get(1).imageAltText);
    }

    @Test
    void emptyAlternativeTextIsAnEmptyText() {
        assertEquals("", parse("![](image.png)").get(0).imageAltText);
        assertEquals("", parse("[![](image.png)](https://example.invalid)").get(0).imageAltText);
    }

    @Test
    void alternativeTextComesWithoutTheBackslashesOfItsEscapes() {
        // ![a\]b \*c\\ \d](image.png)
        assertEquals("a]b *c\\ \\d", parse("![a\\]b \\*c\\\\ \\d](image.png)").get(0).imageAltText);
        // [![\[a\]](image.png)](https://example.invalid)
        assertEquals("[a]", parse("[![\\[a\\]](image.png)](https://example.invalid)").get(0).imageAltText);
    }

    @Test
    void fragmentsWithoutImageHaveNoAlternativeText() {
        List<MarkdownTextFragment> fragments = parse("text ![inline](image.png) [link](https://example.invalid)\n| ![cell](image.png) |\n| --- |");

        for (MarkdownTextFragment fragment : fragments) assertNull(fragment.imageAltText);
        // An image only is an image at the start of a line, which the content of a table cell is
        MarkdownTextFragment cellImage = tableFragment(fragments).tableContext.rows.get(0).cells.get(0).fragments.get(0);
        assertNotNull(cellImage.imageSupplier);
        assertEquals("cell", cellImage.imageAltText);
    }

}
