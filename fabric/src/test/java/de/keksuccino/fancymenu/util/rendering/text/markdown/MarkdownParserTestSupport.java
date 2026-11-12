package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared setup of the Markdown parser tests: a font that works without a running game, and lookups for parsed fragments.
 */
final class MarkdownParserTestSupport {

    /** Every character is one pixel wide, so the width of a fragment is the length of its text. */
    static final Font TEST_FONT = new Font(identifier -> null, false) {
        @Override
        public int width(String text) {
            return text.length();
        }

        @Override
        public int width(FormattedText text) {
            return text.getString().length();
        }
    };

    private MarkdownParserTestSupport() {
    }

    static List<MarkdownTextFragment> parse(String text) {
        return MarkdownParser.parse(new MarkdownRenderer(TEST_FONT), text, true);
    }

    static MarkdownTextFragment fragmentContaining(List<MarkdownTextFragment> fragments, String text) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.contains(text)) return fragment;
        }
        throw new AssertionError("Missing fragment containing: " + text);
    }

    static MarkdownTextFragment tableFragment(List<MarkdownTextFragment> fragments) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.isTable()) return fragment;
        }
        throw new AssertionError("Missing table fragment");
    }

    static List<String> texts(List<MarkdownTextFragment> fragments) {
        List<String> texts = new ArrayList<>();
        for (MarkdownTextFragment fragment : fragments) texts.add(fragment.text);
        return texts;
    }

    static String joinText(List<MarkdownTextFragment> fragments) {
        StringBuilder joined = new StringBuilder();
        for (MarkdownTextFragment fragment : fragments) joined.append(fragment.text);
        return joined.toString();
    }

}
