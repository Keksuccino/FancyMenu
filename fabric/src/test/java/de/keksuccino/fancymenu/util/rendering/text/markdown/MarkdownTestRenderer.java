package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MarkdownRenderer} for tests that works without a running game: it measures text with a fixed width per character,
 * and counts how often it parses its text, lays out its lines and measures the text of a fragment.<br>
 * Tests of other packages can build one to test what they do with a renderer through its public methods.
 */
public final class MarkdownTestRenderer extends MarkdownRenderer {

    /** The UI theme the colors of the neutral look follow. Tests change its colors to act like a theme switch. */
    public final UITheme theme = new UITheme("test", "test");
    /** The characters every font of Minecraft has in this renderer, which are the printable characters of ASCII unless a test sets other ones. */
    MarkdownFontCoverage fontCoverage = fontWithCharacters(" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~");
    int parses = 0;
    int layouts = 0;

    public MarkdownTestRenderer() {
        this(new UITheme[1]);
    }

    private MarkdownTestRenderer(UITheme[] theme) {
        // The supplier gets asked after the renderer got built, which is when the theme of this renderer exists
        super(new TestFont(), () -> theme[0]);
        theme[0] = this.theme;
        // Fonts get read from the resources of the game, so every font of a test has the characters of this definition
        this.textFont.fontCoverage = font -> this.fontCoverage;
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

    /**
     * Returns the characters of a bitmap font that has the given characters.
     */
    @NotNull
    static MarkdownFontCoverage fontWithCharacters(@NotNull String characters) {
        JsonArray rows = new JsonArray();
        rows.add(characters);
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.add("chars", rows);
        JsonArray providers = new JsonArray();
        providers.add(provider);
        JsonObject definition = new JsonObject();
        definition.add("providers", providers);
        return MarkdownFontCoverage.fromDefinitions(List.of(definition));
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
        /**
         * The width of every character of a script without spaces between words, like a Han ideograph or a Kana, which fonts make wider than Latin letters.
         * They are as wide as all other characters while this is 0.
         */
        int spacelessCharWidth = 0;
        /** The texts of all fragments that got measured, in order. */
        final List<String> measuredFragmentTexts = new ArrayList<>();

        private TestFont() {
            super(identifier -> null, false);
        }

        @Override
        public int width(String text) {
            int width = text.length() * this.charWidth;
            if (this.spacelessCharWidth != 0) {
                for (int i = 0; i < text.length(); i++) {
                    if (MarkdownLineBreaks.isSpaceless(text.charAt(i))) width += this.spacelessCharWidth - this.charWidth;
                }
            }
            return width;
        }

        @Override
        public int width(FormattedText text) {
            String string = text.getString();
            this.measuredFragmentTexts.add(string);
            return this.width(string);
        }

    }

}
