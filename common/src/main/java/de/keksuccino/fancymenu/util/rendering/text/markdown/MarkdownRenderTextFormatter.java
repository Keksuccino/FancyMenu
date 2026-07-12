package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.UnaryOperator;

final class MarkdownRenderTextFormatter {

    private static final String AMPERSAND_PREFIX = "&";
    private static final String SECTION_PREFIX = "§";

    private MarkdownRenderTextFormatter() {
    }

    /**
     * Expands placeholders before converting legacy formatting so codes produced dynamically are handled too.
     * This conversion intentionally stays at the Markdown rendering boundary to avoid mutating non-text consumers.
     */
    @NotNull
    static String expandPlaceholdersAndReplaceFormattingCodes(@NotNull String text, @NotNull UnaryOperator<String> placeholderExpander) {
        String expandedText = Objects.requireNonNull(placeholderExpander).apply(Objects.requireNonNull(text));
        return TextFormattingUtils.replaceFormattingCodes(Objects.requireNonNull(expandedText), AMPERSAND_PREFIX, SECTION_PREFIX);
    }

}
