package de.keksuccino.fancymenu.util.rendering.text;

import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IndentedLineWrapperTest {

    /**
     * Every character is 1 wide, so widths are character counts. It measures the text that gets rendered, like the real measurer of UI text does.
     * Components cache that text, see {@link ClientLikeLanguage}.
     */
    private static final ToDoubleFunction<Component> MEASURER = component -> {
        int[] length = new int[] { 0 };
        component.getVisualOrderText().accept((index, style, codepoint) -> {
            length[0]++;
            return true;
        });
        return length[0];
    };
    private static final int GOLD = 0xFFAA00;
    private static final int BLUE = 0x5555FF;

    @Nullable
    private Language previousLanguage;

    @BeforeEach
    void useClientLikeLanguage() {
        this.previousLanguage = Language.getInstance();
        Language.inject(new ClientLikeLanguage());
    }

    @AfterEach
    void restoreLanguage() {
        if (this.previousLanguage != null) Language.inject(this.previousLanguage);
    }

    @Test
    void wrappedLinesOfNestedListItemsStartBelowTheTextAfterTheBullet() {
        List<Float> widths = new ArrayList<>();
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("    • alpha beta gamma delta"), 17, MEASURER, recordingWrapper(widths));

        assertEquals(List.of("    • alpha beta", "gamma delta"), texts(lines));
        assertEquals(List.of(0.0F, 6.0F), indents(lines));
        // The text after the bullet gets wrapped at the width that is left next to the indentation
        assertEquals(List.of(11.0F), widths);
    }

    @Test
    void valueLinesOfActionsGetIndentedBelowTheirTextToo() {
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("      ◦ Value: some long value"), 20, MEASURER, recordingWrapper(new ArrayList<>()));

        assertEquals(List.of("      ◦ Value: some", "long value"), texts(lines));
        assertEquals(List.of(0.0F, 8.0F), indents(lines));
    }

    @Test
    void wrappedLinesOfDashListItemsWithoutIndentationStartBelowTheirText() {
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("- $$key_name = The resolved name"), 20, MEASURER, recordingWrapper(new ArrayList<>()));

        assertEquals(List.of("- $$key_name = The", "resolved name"), texts(lines));
        assertEquals(List.of(0.0F, 2.0F), indents(lines));
    }

    @Test
    void indentedLinesWithoutBulletKeepTheirIndentationWhenWrapped() {
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("  alpha beta gamma"), 12, MEASURER, recordingWrapper(new ArrayList<>()));

        assertEquals(List.of("  alpha beta", "gamma"), texts(lines));
        assertEquals(List.of(0.0F, 2.0F), indents(lines));
    }

    @Test
    void linesWithoutIndentationWrapAtTheLeftEdge() {
        List<Float> widths = new ArrayList<>();
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("alpha beta gamma delta"), 11, MEASURER, recordingWrapper(widths));

        assertEquals(List.of("alpha beta", "gamma delta"), texts(lines));
        assertEquals(List.of(0.0F, 0.0F), indents(lines));
        assertEquals(List.of(11.0F), widths);
    }

    @Test
    void indentationWiderThanHalfTheWidthWrapsLikeALineWithoutIndentation() {
        // Deep nesting in a narrow area would leave almost no room for the text next to the indentation
        List<Float> widths = new ArrayList<>();
        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(Component.literal("            • alpha beta gamma delta epsilon"), 20, MEASURER, recordingWrapper(widths));

        assertEquals(List.of(0.0F, 0.0F), indents(lines));
        assertEquals(List.of(20.0F), widths);
    }

    @Test
    void onlySpacesAndBulletsFollowedByASpaceCountAsIndentation() {
        assertEquals(4, IndentedLineWrapper.getHangingIndentLength("    text"));
        assertEquals(6, IndentedLineWrapper.getHangingIndentLength("    • text"));
        assertEquals(6, IndentedLineWrapper.getHangingIndentLength("  ◦   text"));
        assertEquals(2, IndentedLineWrapper.getHangingIndentLength("* text"));
        // A minus sign of a number is no bullet
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength("-5 degrees"));
        assertEquals(2, IndentedLineWrapper.getHangingIndentLength("  -5 degrees"));
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength("text"));
        // Without text after the indentation, there is nothing to indent
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength("    • "));
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength("   "));
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength(""));
        // Line breaks start new lines anyway, so they are no indentation
        assertEquals(0, IndentedLineWrapper.getHangingIndentLength("\ntext"));
    }

    @Test
    void splittingKeepsTheStyleOfEveryCharacter() {
        MutableComponent line = Component.literal("  • it").setStyle(Style.EMPTY.withColor(GOLD)).append(Component.literal("em").setStyle(Style.EMPTY.withColor(BLUE)));

        MutableComponent[] parts = IndentedLineWrapper.split(line, 4);

        assertEquals("  • ", parts[0].getString());
        assertEquals("item", parts[1].getString());
        assertEquals(List.of(GOLD), colors(parts[0]));
        assertEquals(List.of(GOLD, BLUE), colors(parts[1]));
    }

    @Test
    void theFirstLineKeepsTheStyleOfTheBullet() {
        MutableComponent line = Component.literal("  • ").setStyle(Style.EMPTY.withColor(GOLD)).append(Component.literal("alpha beta").setStyle(Style.EMPTY.withColor(BLUE)));

        List<IndentedLineWrapper.WrappedLine> lines = IndentedLineWrapper.wrap(line, 9, MEASURER, recordingWrapper(new ArrayList<>()));

        assertEquals(List.of("  • alpha", "beta"), texts(lines));
        assertEquals(List.of(GOLD, BLUE), colors(lines.get(0).text()));
        assertEquals(List.of(BLUE), colors(lines.get(1).text()));
    }

    /**
     * Wraps at spaces, measuring with {@link #MEASURER}, and records the widths it got asked to wrap at.
     */
    @NotNull
    private static BiFunction<Component, Float, List<MutableComponent>> recordingWrapper(@NotNull List<Float> widths) {
        return (component, width) -> {
            widths.add(width);
            List<MutableComponent> lines = new ArrayList<>();
            MutableComponent current = Component.empty();
            int currentLength = 0;
            List<StyledWord> words = words(component);
            for (StyledWord word : words) {
                int length = word.text().length();
                if ((currentLength > 0) && ((currentLength + 1 + length) > width)) {
                    lines.add(current);
                    current = Component.empty();
                    currentLength = 0;
                }
                if (currentLength > 0) {
                    current.append(Component.literal(" ").setStyle(word.style()));
                    currentLength++;
                }
                current.append(Component.literal(word.text()).setStyle(word.style()));
                currentLength += length;
            }
            lines.add(current);
            return lines;
        };
    }

    @NotNull
    private static List<StyledWord> words(@NotNull Component component) {
        List<StyledWord> words = new ArrayList<>();
        component.visit((style, text) -> {
            for (String word : text.split(" ")) {
                if (!word.isEmpty()) words.add(new StyledWord(word, style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return words;
    }

    /**
     * The texts of the lines as they get rendered. Checks that every line measures as wide as its text, like the description area measures it.
     */
    @NotNull
    private static List<String> texts(@NotNull List<IndentedLineWrapper.WrappedLine> lines) {
        for (IndentedLineWrapper.WrappedLine line : lines) {
            assertEquals(line.text().getString().length(), MEASURER.applyAsDouble(line.text()), "measured width of '" + line.text().getString() + "'");
        }
        return lines.stream().map(line -> line.text().getString()).toList();
    }

    @NotNull
    private static List<Float> indents(@NotNull List<IndentedLineWrapper.WrappedLine> lines) {
        return lines.stream().map(IndentedLineWrapper.WrappedLine::indent).toList();
    }

    /**
     * The colors of the text, one for each run of characters with the same color.
     */
    @NotNull
    private static List<Integer> colors(@NotNull Component component) {
        List<Integer> colors = new ArrayList<>();
        component.visit((style, text) -> {
            if (text.isEmpty()) return Optional.empty();
            TextColor color = style.getColor();
            assertNotNull(color, "every character has a color");
            if (colors.isEmpty() || (colors.get(colors.size() - 1) != color.getValue())) colors.add(color.getValue());
            return Optional.empty();
        }, Style.EMPTY);
        return colors;
    }

    private record StyledWord(@NotNull String text, @NotNull Style style) {
    }

    /**
     * Orders text for rendering like the language of the game client does: right away when a component asks for it, which components cache
     * until the language changes. So a component that got measured keeps its old text for measuring and rendering when something gets appended to it.
     */
    private static final class ClientLikeLanguage extends Language {

        @Override
        public String getOrDefault(String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean has(String key) {
            return false;
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return false;
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return FormattedBidiReorder.reorder(text, false);
        }

    }

}
