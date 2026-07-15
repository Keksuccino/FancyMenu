package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

final class MarkdownRenderTextNormalizer {

    private static final String NEWLINE_PERCENT = "%n%";
    private static final String NEWLINE = "\n";
    private static final String NEWLINE_R = "\r";
    private static final String NEWLINE_ESCAPED = "\\n";
    private static final String EMPTY_STRING = "";
    private static final String HTML_BREAK = "<br>";

    private MarkdownRenderTextNormalizer() {
    }

    @NotNull
    static String normalize(@NotNull String placeholderExpandedText, boolean removeHtmlBreaks) {
        String normalized = placeholderExpandedText;
        normalized = StringUtils.replace(normalized, NEWLINE_PERCENT, NEWLINE);
        normalized = StringUtils.replace(normalized, NEWLINE_R, NEWLINE);
        normalized = StringUtils.replace(normalized, NEWLINE_ESCAPED, NEWLINE);
        if (removeHtmlBreaks) normalized = StringUtils.replace(normalized, HTML_BREAK, EMPTY_STRING);
        return normalized;
    }

}
