package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CalloutType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.ListItemMarker;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.IntPredicate;

/**
 * The characters and markers of the Markdown syntax, and the checks that read a single piece of that syntax from a text
 * without knowing anything about the text around it.
 */
final class MarkdownSyntax {

    static final char NEWLINE_CHAR = '\n';
    static final String NEWLINE = "\n";
    static final String HEADLINE_PREFIX_BIGGEST = "# ";
    static final String HEADLINE_PREFIX_BIGGER = "## ";
    static final String HEADLINE_PREFIX_BIG = "### ";
    static final char HASHTAG_CHAR = '#';
    static final String FORMATTING_CODE_FONT_PREFIX = "%!!";
    static final String FORMATTING_CODE_FONT_SUFFIX = "%!!%";
    static final char PERCENTAGE_CHAR = '%';
    static final char AMPERSAND_CHAR = '&';
    static final String FORMATTING_CODE_HEX_COLOR_PREFIX = "%#";
    static final String FORMATTING_CODE_HEX_COLOR_SUFFIX = "%#%";
    static final char ASTERISK_CHAR = '*';
    static final String FORMATTING_CODE_BOLD_PREFIX_SUFFIX = "**";
    static final char UNDERSCORE_CHAR = '_';
    static final char BACKSLASH_CHAR = '\\';
    static final char TILDE_CHAR = '~';
    static final String FORMATTING_CODE_STRIKETHROUGH_DOUBLE_PREFIX_SUFFIX = "~~";
    static final char EXCLAMATION_MARK_CHAR = '!';
    static final String FORMATTING_CODE_IMAGE_PREFIX = "![";
    static final String FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX = "[![";
    static final String FORMATTING_CODE_HYPERLINK_INNER_PREFIX = "](";
    private static final String FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX = "click:";
    private static final String FORMATTING_CODE_TEXT_HOVER_EVENT_PREFIX = "hover:";
    private static final char OPEN_ROUND_BRACKETS_CHAR = '(';
    private static final char CLOSE_ROUND_BRACKETS_CHAR = ')';
    static final char OPEN_SQUARE_BRACKETS_CHAR = '[';
    static final char CLOSE_SQUARE_BRACKETS_CHAR = ']';
    private static final String OPEN_SQUARE_BRACKETS = "[";
    static final char GREATER_THAN_CHAR = '>';
    static final String FORMATTING_CODE_QUOTE_PREFIX = "> ";
    private static final String FORMATTING_CODE_CALLOUT_PREFIX = "[!";
    static final CalloutType[] CALLOUT_TYPES = CalloutType.values();
    static final String SPACE = " ";
    static final char SPACE_CHAR = ' ';
    static final char MINUS_CHAR = '-';
    private static final char PLUS_CHAR = '+';
    private static final char DOT_CHAR = '.';
    private static final String DOT = ".";
    private static final String CLOSE_ROUND_BRACKETS = ")";
    /** The indent of the items of the deepest list level, which is level 10. Markers behind a longer indent start no list item. */
    static final int MAX_LIST_ITEM_INDENT = 19;
    /** Longer numbers start no list item, like in CommonMark. */
    private static final int MAX_LIST_ITEM_NUMBER_DIGITS = 9;
    static final String FORMATTING_CODE_SEPARATION_LINE_PREFIX = "---";
    static final char GRAVE_ACCENT_CHAR = '`';
    static final String FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX = "```";
    static final String FORMATTING_CODE_CODE_BLOCK_SINGLE_LINE_EMPTY = "``";
    static final char SEMICOLON_CHAR = ';';
    static final String FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX = ";;";
    static final String FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX = ";;;";
    static final char CIRCUMFLEX_CHAR = '^';
    static final String FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX = "^^^";
    static final char VERTICAL_BAR_CHAR = '|';
    static final String FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX = "|||";
    static final String VERTICAL_BAR = "|";
    private static final char OPEN_CURLY_BRACKET_CHAR = '{';
    private static final char CLOSE_CURLY_BRACKET_CHAR = '}';
    private static final CharacterFilter RESOURCE_NAME_FILTER = CharacterFilter.buildResourceNameFilter();
    static final char COLON_CHAR = ':';
    static final String COLON = ":";
    static final IntPredicate IS_SPACE = character -> character == SPACE_CHAR;
    /** Accepts the characters {@link String#trim()} removes. */
    static final IntPredicate IS_TRIMMED_WHITESPACE = character -> character <= SPACE_CHAR;
    static final IntPredicate IS_SEPARATION_LINE_CHARACTER = character -> (character == MINUS_CHAR) || (character == SPACE_CHAR);
    private static final IntPredicate IS_RESOURCE_NAME_CHARACTER = character -> RESOURCE_NAME_FILTER.isAllowedChar((char) character);

    private MarkdownSyntax() {
    }

    static boolean isEscapableMarkdownCharacter(char character) {
        switch (character) {
            case BACKSLASH_CHAR:
            case GRAVE_ACCENT_CHAR:
            case ASTERISK_CHAR:
            case UNDERSCORE_CHAR:
            case OPEN_CURLY_BRACKET_CHAR:
            case CLOSE_CURLY_BRACKET_CHAR:
            case OPEN_SQUARE_BRACKETS_CHAR:
            case CLOSE_SQUARE_BRACKETS_CHAR:
            case OPEN_ROUND_BRACKETS_CHAR:
            case CLOSE_ROUND_BRACKETS_CHAR:
            case HASHTAG_CHAR:
            case PLUS_CHAR:
            case MINUS_CHAR:
            case DOT_CHAR:
            case EXCLAMATION_MARK_CHAR:
            case VERTICAL_BAR_CHAR:
            case GREATER_THAN_CHAR:
            case TILDE_CHAR:
            case CIRCUMFLEX_CHAR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns if a Minecraft formatting code starts at the given index: a section sign or an ampersand with the character of a Vanilla formatting
     * behind it, or a section sign with the character of a custom text color formatter behind it.
     */
    static boolean isFormattingCode(@NotNull String text, int index) {
        char prefix = text.charAt(index);
        if (prefix == ChatFormatting.PREFIX_CODE) return (MinecraftFormattingState.getSectionFormattingAt(text, index) != null) || (MinecraftFormattingState.getCustomFormattingAt(text, index) != null);
        return (prefix == AMPERSAND_CHAR) && (MinecraftFormattingState.getAmpersandFormattingAt(text, index) != null);
    }

    static boolean isCharacterEscaped(@NotNull String markdownText, int index) {
        if (index <= 0) return false;
        int backslashCount = 0;
        for (int i = index - 1; i >= 0; i--) {
            if (markdownText.charAt(i) == BACKSLASH_CHAR) {
                backslashCount++;
            } else {
                break;
            }
        }
        return (backslashCount % 2) == 1;
    }

    /**
     * Returns the text between the two indexes without the backslashes of its escaped characters.
     */
    @NotNull
    static String unescape(@NotNull String text, int start, int end) {
        int firstBackslash = indexOf(text, BACKSLASH_CHAR, start, end);
        if (firstBackslash == -1) return text.substring(start, end);
        StringBuilder unescaped = new StringBuilder(end - start).append(text, start, firstBackslash);
        for (int i = firstBackslash; i < end; i++) {
            // The character behind the backslash of an escape gets added as it is, even if it is a backslash as well
            if ((text.charAt(i) == BACKSLASH_CHAR) && (i + 1 < end) && isEscapableMarkdownCharacter(text.charAt(i + 1))) i++;
            unescaped.append(text.charAt(i));
        }
        return unescaped.toString();
    }

    /**
     * Returns the index of the percent sign that ends the font prefix ("%!!font_name%") starting at the given index, or -1 if no font prefix starts there.
     *
     * @param lineEnd The index of the line break that ends the line of the given index, or the text length if it is the last line.
     */
    static int getFontPrefixEnd(@NotNull String text, int index, int lineEnd) {
        if (!text.startsWith(FORMATTING_CODE_FONT_PREFIX, index) || text.startsWith(FORMATTING_CODE_FONT_SUFFIX, index)) return -1;
        int nameStart = index + FORMATTING_CODE_FONT_PREFIX.length();
        int nameEnd = indexOf(text, PERCENTAGE_CHAR, nameStart, lineEnd);
        return ((nameEnd != -1) && containsOnly(text, nameStart, nameEnd, IS_RESOURCE_NAME_CHARACTER)) ? nameEnd : -1;
    }

    /**
     * Returns the index of the line break that ends the line of the given index, or the text length if it is the last line.
     */
    static int getLineEnd(@NotNull String text, int index) {
        int lineEnd = text.indexOf(NEWLINE_CHAR, index);
        return (lineEnd != -1) ? lineEnd : text.length();
    }

    /**
     * Returns the index of the first occurrence of the character between the two indexes, or -1 if there is none.<br>
     * Other than {@link String#indexOf(int, int)}, this never reads further than the end index.
     */
    static int indexOf(@NotNull String text, char character, int start, int end) {
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == character) return i;
        }
        return -1;
    }

    /**
     * Returns if the text between the two indexes has no character the given check rejects, which includes having no characters at all.
     */
    static boolean containsOnly(@NotNull String text, int start, int end, @NotNull IntPredicate allowedCharacter) {
        for (int i = start; i < end; i++) {
            if (!allowedCharacter.test(text.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Returns the type of the callout marker ("[!NOTE]") that fills the text between the two indexes, or NULL if that text is no callout marker.<br>
     * The case of the type does not matter, and nothing but spaces may follow the marker.
     */
    @Nullable
    static CalloutType getCalloutType(@NotNull String text, int start, int end) {
        if (!text.startsWith(FORMATTING_CODE_CALLOUT_PREFIX, start)) return null;
        int typeStart = start + FORMATTING_CODE_CALLOUT_PREFIX.length();
        for (CalloutType type : CALLOUT_TYPES) {
            String name = type.name();
            int typeEnd = typeStart + name.length();
            if ((typeEnd < end) && (text.charAt(typeEnd) == CLOSE_SQUARE_BRACKETS_CHAR) && text.regionMatches(true, typeStart, name, 0, name.length()) && containsOnly(text, typeEnd + 1, end, IS_SPACE)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns the index of the space behind the list item marker that starts at the given index, or -1 if no list item starts there.<br>
     * A list item starts with its marker and a space. The marker is "-", or one of "+", "*" and "~" if the extended bullet markers are enabled,
     * or a number with a "." or ")" behind it if numbered lists are enabled.
     */
    static int getListItemMarkerEnd(@NotNull String text, int markerStart, boolean extendedBulletListMarkers, boolean numberedLists) {
        if (markerStart >= text.length()) return -1;
        int markerEnd;
        switch (getListItemMarker(text.charAt(markerStart))) {
            case DASH:
                markerEnd = markerStart + 1;
                break;
            case PLUS:
            case ASTERISK:
            case TILDE:
                if (!extendedBulletListMarkers) return -1;
                markerEnd = markerStart + 1;
                break;
            case NUMBER:
                if (!numberedLists) return -1;
                // The number ends at its "." or ")"
                int digitsEnd = markerStart + 1;
                while ((digitsEnd < text.length()) && ((digitsEnd - markerStart) < MAX_LIST_ITEM_NUMBER_DIGITS) && isDigit(text.charAt(digitsEnd))) digitsEnd++;
                if (!text.startsWith(DOT, digitsEnd) && !text.startsWith(CLOSE_ROUND_BRACKETS, digitsEnd)) return -1;
                markerEnd = digitsEnd + 1;
                break;
            default:
                return -1;
        }
        return text.startsWith(SPACE, markerEnd) ? markerEnd : -1;
    }

    /**
     * Returns the marker of the list items that start with the given character, or NONE if no list item starts with it.
     */
    @NotNull
    static ListItemMarker getListItemMarker(char character) {
        switch (character) {
            case MINUS_CHAR:
                return ListItemMarker.DASH;
            case PLUS_CHAR:
                return ListItemMarker.PLUS;
            case ASTERISK_CHAR:
                return ListItemMarker.ASTERISK;
            case TILDE_CHAR:
                return ListItemMarker.TILDE;
            default:
                return isDigit(character) ? ListItemMarker.NUMBER : ListItemMarker.NONE;
        }
    }

    /**
     * Returns if the character is one of the digits 0 to 9. Digits of other writing systems start no numbered list item.
     */
    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    @Nullable
    static HyperlinkImage getHyperlinkImageFromLine(@NotNull String line) {
        if (line.startsWith(FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX)) {
            int imageStart = line.indexOf(FORMATTING_CODE_IMAGE_PREFIX, 1);
            while (imageStart != -1) {
                int imageLabelStart = imageStart + FORMATTING_CODE_IMAGE_PREFIX.length();
                int imageLabelEnd = getLinkLabelEnd(line, imageLabelStart);
                int imageSourceStart = imageLabelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length();
                int imageEnd = (imageLabelEnd != -1) ? getLinkTargetEnd(line, imageSourceStart) : -1;
                if (imageEnd != -1) {
                    // The hyperlink is what remains of the line without the image. The image ends at the bracket that closes its source,
                    // which is not the first closing round bracket behind its start if its label holds round brackets as well.
                    String hyperLink = getHyperlinkFromLine(line.substring(0, imageStart) + line.substring(imageEnd + 1), 0);
                    return (hyperLink != null) ? new HyperlinkImage(line.substring(imageSourceStart, imageEnd), unescape(line, imageLabelStart, imageLabelEnd), hyperLink) : null;
                }
                imageStart = line.indexOf(FORMATTING_CODE_IMAGE_PREFIX, imageStart + 1);
            }
        }
        return null;
    }

    /**
     * An image that is a hyperlink ("[![alternative text](image source)](link)").
     */
    record HyperlinkImage(@NotNull String imageSource, @NotNull String imageAltText, @NotNull String link) {
    }

    @Nullable
    static String getTextClickEventId(@NotNull String hyperlink) {
        if (StringUtils.startsWithIgnoreCase(hyperlink, FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX)) {
            String id = StringUtils.trim(StringUtils.substring(hyperlink, FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX.length()));
            if (!id.isEmpty()) {
                return id;
            }
        }
        return null;
    }

    @Nullable
    static String getTextHoverEventId(@NotNull String hyperlink) {
        if (StringUtils.startsWithIgnoreCase(hyperlink, FORMATTING_CODE_TEXT_HOVER_EVENT_PREFIX)) {
            String id = StringUtils.trim(StringUtils.substring(hyperlink, FORMATTING_CODE_TEXT_HOVER_EVENT_PREFIX.length()));
            if (!id.isEmpty()) {
                return id;
            }
        }
        return null;
    }

    /**
     * Returns the target of the hyperlink that starts at the given index, or NULL if there is no valid hyperlink.
     */
    @Nullable
    private static String getHyperlinkFromLine(@NotNull String text, int hyperlinkStart) {
        if (!text.startsWith(OPEN_SQUARE_BRACKETS, hyperlinkStart)) return null;
        return getLinkTarget(text, hyperlinkStart + OPEN_SQUARE_BRACKETS.length());
    }

    /**
     * Returns the target of the link ("label](target)") whose label starts at the given index, or NULL if there is no valid link.
     */
    @Nullable
    private static String getLinkTarget(@NotNull String text, int labelStart) {
        int labelEnd = getLinkLabelEnd(text, labelStart);
        if (labelEnd == -1) return null;
        int targetStart = labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length();
        int targetEnd = getLinkTargetEnd(text, targetStart);
        return (targetEnd != -1) ? text.substring(targetStart, targetEnd) : null;
    }

    /**
     * Returns the index of the square bracket that ends the link label starting at the given index, or -1 if there is no valid label.<br>
     * A valid label stays in its line and ends at the first square bracket that is not escaped. That bracket has to be a closing
     * one with the target of the link right behind it ("](").
     *
     * <p>Escaped characters get skipped the same way the parser reads them, so the parser always reaches the bracket this returns.</p>
     *
     * <p>The search stops at the first square bracket or line break it reaches, so the label search of one link ends before the label of the
     * next link starts. {@link #getLinkTargetEnd(String, int)} stops at the first closing square bracket or line break at the latest, which is in front
     * of the next target. Together, this keeps the time for all searches of a text linear to its length, no matter how many brackets it holds.</p>
     */
    static int getLinkLabelEnd(@NotNull String text, int labelStart) {
        int length = text.length();
        for (int i = labelStart; i < length; i++) {
            char c = text.charAt(i);
            if ((c == NEWLINE_CHAR) || (c == OPEN_SQUARE_BRACKETS_CHAR)) return -1;
            if (c == CLOSE_SQUARE_BRACKETS_CHAR) return text.startsWith(FORMATTING_CODE_HYPERLINK_INNER_PREFIX, i) ? i : -1;
            if ((c == BACKSLASH_CHAR) && (i + 1 < length) && isEscapableMarkdownCharacter(text.charAt(i + 1))) i++;
        }
        return -1;
    }

    /**
     * Returns the index of the round bracket that ends the link target starting at the given index, or -1 if there is no valid target.<br>
     * A valid target stays in its line and holds no closing square bracket. Round brackets inside of it have to come in pairs,
     * like in "wiki/Name_(disambiguation)", so the bracket that ends the target is the first closing one without an opening one in the target.
     */
    static int getLinkTargetEnd(@NotNull String text, int targetStart) {
        int length = text.length();
        int openBrackets = 0;
        for (int i = targetStart; i < length; i++) {
            char c = text.charAt(i);
            if (c == CLOSE_ROUND_BRACKETS_CHAR) {
                if (openBrackets == 0) return i;
                openBrackets--;
            } else if (c == OPEN_ROUND_BRACKETS_CHAR) {
                openBrackets++;
            } else if ((c == NEWLINE_CHAR) || (c == CLOSE_SQUARE_BRACKETS_CHAR)) {
                return -1;
            }
        }
        return -1;
    }

}
