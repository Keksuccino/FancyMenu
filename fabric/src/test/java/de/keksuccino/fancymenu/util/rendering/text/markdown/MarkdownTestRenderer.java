package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MarkdownRenderer} for tests that works without a running game: it measures text with a fixed width per character,
 * and counts how often it parses its text, lays out its lines and measures the text of a fragment.
 */
final class MarkdownTestRenderer extends MarkdownRenderer {

    /** The UI theme the colors of the neutral look follow. Tests change its colors to act like a theme switch. */
    final UITheme theme = new UITheme("test", "test");
    int parses = 0;
    int layouts = 0;

    MarkdownTestRenderer() {
        this(new UITheme[1]);
    }

    private MarkdownTestRenderer(UITheme[] theme) {
        // The supplier gets asked after the renderer got built, which is when the theme of this renderer exists
        super(new TestFont(), () -> theme[0]);
        theme[0] = this.theme;
    }

    MarkdownTestRenderer(@NotNull String text, float optimalWidth) {
        this();
        this.setText(text);
        this.setOptimalWidth(optimalWidth);
    }

    @Override
    protected void rebuildFragments() {
        this.parses++;
        super.rebuildFragments();
    }

    @Override
    protected void rebuildLines() {
        this.layouts++;
        super.rebuildLines();
    }

    @NotNull
    TestFont testFont() {
        return (TestFont) this.font;
    }

    @NotNull
    MarkdownTextFragment fragmentContaining(@NotNull String text) {
        return fragmentContaining(this.fragments, text);
    }

    @NotNull
    static MarkdownTextFragment fragmentContaining(@NotNull List<MarkdownTextFragment> fragments, @NotNull String text) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.contains(text)) return fragment;
        }
        throw new AssertionError("Missing fragment containing: " + text);
    }

    @NotNull
    MarkdownTextFragment tableFragment(int index) {
        int found = 0;
        for (MarkdownTextFragment fragment : this.fragments) {
            if (fragment.isTable() && (found++ == index)) return fragment;
        }
        throw new AssertionError("Missing table fragment: " + index);
    }

    static final class TestFont extends Font {

        int charWidth = 6;
        /** The texts of all fragments that got measured, in order. */
        final List<String> measuredFragmentTexts = new ArrayList<>();

        private TestFont() {
            super(null, false);
        }

        @Override
        public int width(String text) {
            return text.length() * this.charWidth;
        }

        @Override
        public int width(FormattedText text) {
            String string = text.getString();
            this.measuredFragmentTexts.add(string);
            return string.length() * this.charWidth;
        }

    }

}
