package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.joinText;
import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownParserTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The markers of fonts and colors start and end with a percent sign, so the last percent sign of one marker can be the first one
 * of the next marker. Both markers work then, and neither a font nor a color stays open because its marker got lost.
 */
class MarkdownParserSharedPercentSignTest {

    /**
     * The words say how they have to look: "font", "color", "both" or "none".
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // The prefix of a color ends where the suffix of the font starts, and the other way around
            "%!!uniform%font %#ff0000%!!%color%#% none",
            "%#ff0000%color %!!uniform%#%font%!!% none",
            // Both suffixes share a percent sign
            "%!!uniform%%#ff0000%both%!!%#% none",
            "%#ff0000%%!!uniform%both%#%!!% none",
            // A suffix ends where the prefix of the other format starts
            "%!!uniform%font%!!%#ff0000%color%#% none",
            "%#ff0000%color%#%!!uniform%font%!!% none",
            // A suffix ends where the next prefix of the same format starts
            "%!!uniform%font%!!%!!default%font%!!% none",
            "%#ff0000%color%#%#00ff00%color%#% none",
            // Both prefixes share a percent sign
            "%!!uniform%#ff0000%both%#%!!% none",
            "%#ff0000%!!uniform%both%!!%#% none",
            // Markers that share nothing
            "%!!uniform%font%!!% %#ff0000%color%#% none",
            "%!!uniform%font %#ff0000%both%#% font%!!% none"
    })
    void markersThatShareAPercentSignAllWork(String text) {
        List<MarkdownTextFragment> fragments = parse(text);

        // Nothing of the markers is left in the text
        assertEquals(text.replaceAll("%(!!|#)[a-z0-9]*", "").replace("%", ""), joinText(fragments));
        for (MarkdownTextFragment fragment : fragments) {
            String word = fragment.text.trim();
            if (word.isEmpty()) continue;
            assertEquals(word.equals("font") || word.equals("both"), fragment.font != null, () -> text + " <" + fragment.text + ">");
            assertEquals(word.equals("color") || word.equals("both"), fragment.textColor != null, () -> text + " <" + fragment.text + ">");
        }
    }

    @Test
    void percentSignThatEndsAMarkerAndStartsNoOtherOneIsNoText() {
        List<MarkdownTextFragment> fragments = parse("%!!uniform%font%!!%text %#ff0000%color%#%text");

        assertEquals("fonttext colortext", joinText(fragments));
    }

}
