package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CalloutType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CodeBlockContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.ListItemMarker;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;

public class MarkdownParser {

    private static final char NEWLINE_CHAR = '\n';
    private static final String NEWLINE = "\n";
    private static final String HEADLINE_PREFIX_BIGGEST = "# ";
    private static final String HEADLINE_PREFIX_BIGGER = "## ";
    private static final String HEADLINE_PREFIX_BIG = "### ";
    private static final char HASHTAG_CHAR = '#';
    private static final String FORMATTING_CODE_FONT_PREFIX = "%!!";
    private static final String FORMATTING_CODE_FONT_SUFFIX = "%!!%";
    private static final char PERCENTAGE_CHAR = '%';
    private static final String FORMATTING_CODE_HEX_COLOR_PREFIX = "%#";
    private static final String FORMATTING_CODE_HEX_COLOR_SUFFIX = "%#%";
    private static final char ASTERISK_CHAR = '*';
    private static final String FORMATTING_CODE_BOLD_PREFIX_SUFFIX = "**";
    private static final char UNDERSCORE_CHAR = '_';
    private static final char BACKSLASH_CHAR = '\\';
    private static final char TILDE_CHAR = '~';
    private static final String FORMATTING_CODE_STRIKETHROUGH_DOUBLE_PREFIX_SUFFIX = "~~";
    private static final char EXCLAMATION_MARK_CHAR = '!';
    private static final String FORMATTING_CODE_IMAGE_PREFIX = "![";
    private static final String FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX = "[![";
    private static final String FORMATTING_CODE_HYPERLINK_INNER_PREFIX = "](";
    private static final String FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX = "click:";
    private static final String FORMATTING_CODE_TEXT_HOVER_EVENT_PREFIX = "hover:";
    private static final char OPEN_ROUND_BRACKETS_CHAR = '(';
    private static final char CLOSE_ROUND_BRACKETS_CHAR = ')';
    private static final char OPEN_SQUARE_BRACKETS_CHAR = '[';
    private static final char CLOSE_SQUARE_BRACKETS_CHAR = ']';
    private static final String OPEN_SQUARE_BRACKETS = "[";
    private static final char GREATER_THAN_CHAR = '>';
    private static final String FORMATTING_CODE_QUOTE_PREFIX = "> ";
    private static final String FORMATTING_CODE_CALLOUT_PREFIX = "[!";
    private static final CalloutType[] CALLOUT_TYPES = CalloutType.values();
    private static final String SPACE = " ";
    private static final char SPACE_CHAR = ' ';
    private static final char MINUS_CHAR = '-';
    private static final char PLUS_CHAR = '+';
    private static final char DOT_CHAR = '.';
    private static final String DOT = ".";
    private static final String CLOSE_ROUND_BRACKETS = ")";
    /** The indent of the items of the deepest list level, which is level 10. Markers behind a longer indent start no list item. */
    private static final int MAX_LIST_ITEM_INDENT = 19;
    /** Longer numbers start no list item, like in CommonMark. */
    private static final int MAX_LIST_ITEM_NUMBER_DIGITS = 9;
    private static final String FORMATTING_CODE_SEPARATION_LINE_PREFIX = "---";
    private static final char GRAVE_ACCENT_CHAR = '`';
    private static final String FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX = "```";
    private static final String FORMATTING_CODE_CODE_BLOCK_SINGLE_LINE_EMPTY = "``";
    private static final char SEMICOLON_CHAR = ';';
    private static final String FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX = ";;";
    private static final String FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX = ";;;";
    private static final char CIRCUMFLEX_CHAR = '^';
    private static final String FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX = "^^^";
    private static final char VERTICAL_BAR_CHAR = '|';
    private static final String FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX = "|||";
    private static final String VERTICAL_BAR = "|";
    private static final char OPEN_CURLY_BRACKET_CHAR = '{';
    private static final char CLOSE_CURLY_BRACKET_CHAR = '}';
    private static final CharacterFilter RESOURCE_NAME_FILTER = CharacterFilter.buildResourceNameFilter();
    private static final char COLON_CHAR = ':';
    private static final String COLON = ":";
    private static final IntPredicate IS_SPACE = character -> character == SPACE_CHAR;
    /** Accepts the characters {@link String#trim()} removes. */
    private static final IntPredicate IS_TRIMMED_WHITESPACE = character -> character <= SPACE_CHAR;
    private static final IntPredicate IS_SEPARATION_LINE_CHARACTER = character -> (character == MINUS_CHAR) || (character == SPACE_CHAR);
    private static final IntPredicate IS_RESOURCE_NAME_CHARACTER = character -> RESOURCE_NAME_FILTER.isAllowedChar((char) character);

    @NotNull
    public static List<MarkdownTextFragment> parse(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, boolean parseMarkdown) {

        Objects.requireNonNull(renderer);
        Objects.requireNonNull(markdownText);

        List<Table> tables = Collections.emptyList();

        if (parseMarkdown) {
            // Line breaks at the end of the text add no lines to it
            int end = markdownText.length();
            while ((end > 0) && (markdownText.charAt(end - 1) == NEWLINE_CHAR)) end--;
            markdownText = markdownText.substring(0, end);
            tables = findTables(renderer, markdownText);
        }

        return parseFragments(renderer, markdownText, parseMarkdown, tables);

    }

    /**
     * Parses text that has its tables found already.
     *
     * <p>The loop reads the text once and never copies the rest of it. Everything that looks ahead is either limited to
     * the current line, stops at the first character that settles the answer, or goes through {@link MarkerLookahead}.
     * This keeps the parsing time linear to the text length, so every new lookahead has to follow the same rules.</p>
     *
     * <p>The loop jumps over the lines of a table and adds one fragment for all of them. A table that it reaches inside a code block
     * or plain text is no table, but text of that block, so the loop reads its lines like every other line then.</p>
     *
     * @param tables The tables of the text, in the order of their lines.
     */
    @NotNull
    protected static List<MarkdownTextFragment> parseFragments(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, boolean parseMarkdown, @NotNull List<Table> tables) {

        List<MarkdownTextFragment> fragments = new ArrayList<>();

        FragmentBuilder builder = new FragmentBuilder(renderer);
        boolean extendedBulletListMarkers = renderer.isExtendedBulletListMarkersEnabled();
        MarkerLookahead lookahead = new MarkerLookahead(markdownText, extendedBulletListMarkers, tables);
        boolean numberedLists = renderer.isNumberedListsEnabled();
        boolean callouts = renderer.isCalloutsEnabled();
        boolean queueNewLine = true;
        boolean italicUnderscore = false;
        boolean strikethroughDouble = false;
        int charsToSkip = 0;
        boolean preserveStartOfLineWhileSkipping = false;
        boolean skipLine = false;
        boolean lineHandledByWholeLineConstruct = false;
        MarkdownTextFragment lastBuiltFragment = null;
        boolean lastLineWasHeadline = false;
        //Index of the square bracket that ends the label of the open link, and of the round bracket that ends its target
        int linkLabelEnd = -1;
        int linkTargetEnd = -1;

        int textLength = markdownText.length();
        //Index of the line break that ends the line of the current index, or the text length if it is the last line
        int lineEnd = -1;
        //The tables follow the order of their lines, so the loop only ever has to look out for the next one
        int nextTable = 0;
        int nextTableStart = tables.isEmpty() ? -1 : tables.get(0).start();
        //If the loop reads one more line break behind the text, which a table that ends the text has behind it when the loop reads its lines as text
        boolean lineBreakBehindText = false;

        for (int index = 0; (index < textLength) || lineBreakBehindText; index++) {

            char c;
            if (index < textLength) {
                c = markdownText.charAt(index);
            } else {
                c = NEWLINE_CHAR;
                lineBreakBehindText = false;
            }

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;
            if (isStartOfLine) lineHandledByWholeLineConstruct = false;

            if (lineEnd < index) lineEnd = getLineEnd(markdownText, index);
            //Length of the part of the current line that starts at the current index
            int remainingLineLength = lineEnd - index;

            // Check for a table
            if (index == nextTableStart) {
                Table table = tables.get(nextTable);
                nextTable++;
                nextTableStart = (nextTable < tables.size()) ? tables.get(nextTable).start() : -1;
                if ((builder.codeBlockContext != null) || builder.plainText) {
                    // Inside a code block or plain text, the lines of the table are text like every other line of it
                    lookahead.readTableAsText(table);
                    lineBreakBehindText = (table.end() == textLength);
                } else {
                    MarkdownTextFragment tableFragment = new MarkdownTextFragment(renderer, "[TABLE]");
                    tableFragment.table = true;
                    tableFragment.tableContext = table.tableContext();
                    tableFragment.naturalLineBreakAfter = true;
                    // Inherit current alignment from builder
                    tableFragment.alignment = builder.alignment;
                    lastBuiltFragment = addFragment(fragments, tableFragment);
                    lineHandledByWholeLineConstruct = true;
                    builder.headlineType = HeadlineType.NONE;
                    builder.separationLine = false;
                    queueNewLine = true;
                    // The loop continues behind the line break that ends the last line of the table
                    index = table.end();
                    continue;
                }
            }

            //Skip empty line under headline
            if ((c == NEWLINE_CHAR) && lastLineWasHeadline) {
                lastLineWasHeadline = false;
                int nextLineStart = index + 1;
                if (containsOnly(markdownText, nextLineStart, getLineEnd(markdownText, nextLineStart), IS_SPACE)) {
                    continue;
                }
            }

            //Skip Chars
            if (charsToSkip > 0) {
                charsToSkip--;
                if (preserveStartOfLineWhileSkipping) {
                    queueNewLine = true;
                    if (charsToSkip == 0) preserveStartOfLineWhileSkipping = false;
                }
                continue;
            }

            //Skip Line
            if ((c != NEWLINE_CHAR) && skipLine) {
                continue;
            }
            if ((c == NEWLINE_CHAR) && skipLine) {
                lineHandledByWholeLineConstruct = true;
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                skipLine = false;
                queueNewLine = true;
                continue;
            }

            // Parse formatting before Markdown so its state survives the word-sized fragments built below. Ampersand
            // codes are handled here, after Markdown destinations are skipped, so URL query parameters stay intact.
            ChatFormatting vanillaFormatting = c == ChatFormatting.PREFIX_CODE ? MinecraftFormattingState.getSectionFormattingAt(markdownText, index) : c == '&' ? MinecraftFormattingState.getAmpersandFormattingAt(markdownText, index) : null;
            TextColorFormatter customFormatting = c == ChatFormatting.PREFIX_CODE && vanillaFormatting == null ? MinecraftFormattingState.getCustomFormattingAt(markdownText, index) : null;
            if ((vanillaFormatting != null) || (customFormatting != null)) {
                if (!builder.text.isEmpty()) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                }
                builder.minecraftFormatting = vanillaFormatting != null ? builder.minecraftFormatting.apply(vanillaFormatting) : builder.minecraftFormatting.apply(Objects.requireNonNull(customFormatting));
                // A following isolated space must be built with the new formatting state instead of being folded
                // into the fragment before this formatting boundary by the whitespace optimization below.
                lastBuiltFragment = null;
                charsToSkip = 1;
                if (isStartOfLine) {
                    queueNewLine = true;
                    preserveStartOfLineWhileSkipping = true;
                }
                continue;
            }

            if (parseMarkdown) {

                //Handle Escapes
                if ((c == BACKSLASH_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    int nextIndex = index + 1;
                    if (nextIndex < textLength) {
                        char nextChar = markdownText.charAt(nextIndex);
                        if (isEscapableMarkdownCharacter(nextChar)) {
                            builder.text.append(nextChar);
                            charsToSkip = 1;
                            continue;
                        }
                    }
                }

                //Handle Headline
                if (isStartOfLine && (c == HASHTAG_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (builder.headlineType == HeadlineType.NONE) {
                        if (markdownText.startsWith(HEADLINE_PREFIX_BIGGEST, index)) {
                            builder.headlineType = HeadlineType.BIGGEST;
                            charsToSkip = 1;
                        }
                        if (markdownText.startsWith(HEADLINE_PREFIX_BIGGER, index)) {
                            builder.headlineType = HeadlineType.BIGGER;
                            charsToSkip = 2;
                        }
                        if (markdownText.startsWith(HEADLINE_PREFIX_BIG, index)) {
                            builder.headlineType = HeadlineType.BIG;
                            charsToSkip = 3;
                        }
                        if (builder.headlineType != HeadlineType.NONE) {
                            lastLineWasHeadline = true;
                            continue;
                        }
                    }
                }

                //Handle Fonts
                if (c == PERCENTAGE_CHAR) {
                    if (markdownText.startsWith(FORMATTING_CODE_FONT_PREFIX, index) && !markdownText.startsWith(FORMATTING_CODE_FONT_SUFFIX, index) && (builder.font == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                        int fontNameStart = index + FORMATTING_CODE_FONT_PREFIX.length();
                        int fontNameEnd = indexOf(markdownText, PERCENTAGE_CHAR, fontNameStart, lineEnd);
                        if ((fontNameEnd != -1) && containsOnly(markdownText, fontNameStart, fontNameEnd, IS_RESOURCE_NAME_CHARACTER) && lookahead.existsFrom(LookaheadMarker.FONT_SUFFIX, index)) {
                            ResourceLocation font = null;
                            try {
                                font = ResourceLocation.parse(markdownText.substring(fontNameStart, fontNameEnd));
                            } catch (Exception ignore) {}
                            if (font != null) {
                                if (isStartOfLine || !builder.text.isEmpty()) {
                                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                }
                                builder.font = font;
                                charsToSkip = fontNameEnd - index;
                                continue;
                            }
                        }
                    }
                    if (markdownText.startsWith(FORMATTING_CODE_FONT_SUFFIX, index) && (builder.font != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.font = null;
                        charsToSkip = 3;
                        continue;
                    }
                }

                //Handle Color Formatting
                if (c == PERCENTAGE_CHAR) {
                    if (markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_PREFIX, index) && !markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_SUFFIX, index) && (builder.textColor == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                        int colorStart = index + FORMATTING_CODE_HEX_COLOR_PREFIX.length();
                        int colorEnd = indexOf(markdownText, PERCENTAGE_CHAR, colorStart, lineEnd);
                        if ((colorEnd != -1) && lookahead.existsFrom(LookaheadMarker.HEX_COLOR_SUFFIX, index)) {
                            DrawableColor color = DrawableColor.ofHtml(markdownText.substring(colorStart, colorEnd));
                            if (color != DrawableColor.EMPTY) {
                                if (isStartOfLine || !builder.text.isEmpty()) {
                                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                }
                                builder.textColor = color;
                                charsToSkip = colorEnd - index;
                                continue;
                            }
                        }
                    }
                    if (markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_SUFFIX, index) && (builder.textColor != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.textColor = null;
                        charsToSkip = 2;
                        continue;
                    }
                }

                // Handle List Items (all levels)
                // Markers that are format markers as well start a list item first, so this has to come before the formats
                if (isStartOfLine && (builder.codeBlockContext == null) && !builder.plainText) {
                    // The marker of an item follows 2 spaces for every level below the first one
                    int markerStart = index;
                    while ((markerStart < lineEnd) && (markdownText.charAt(markerStart) == SPACE_CHAR)) markerStart++;
                    int markerEnd = getListItemMarkerEnd(markdownText, markerStart, extendedBulletListMarkers, numberedLists);
                    if (markerEnd != -1) {
                        int indent = markerStart - index;

                        if (indent <= MAX_LIST_ITEM_INDENT) {
                            // Skips the indent, the marker and the space behind it. The current char is consumed already.
                            charsToSkip = markerEnd - index;

                            // Configure builder
                            builder.bulletListLevel = (indent / 2) + 1;
                            builder.bulletListItemStart = true;
                            builder.listItemMarker = getListItemMarker(markdownText.charAt(markerStart));
                            builder.listItemNumber = (builder.listItemMarker == ListItemMarker.NUMBER) ? markdownText.substring(markerStart, markerEnd) : null;

                            continue;
                        }
                    }
                }

                //Handle Bold
                if ((c == ASTERISK_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.bold && (builder.codeBlockContext == null) && !builder.plainText) {
                        if (markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index) && lookahead.existsFrom(LookaheadMarker.BOLD, index + 2)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.bold = true;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                    if (builder.bold) {
                        if (markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.bold = false;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                }

                //Handle Italic Underscore
                if ((c == UNDERSCORE_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.italic && (builder.codeBlockContext == null) && !builder.plainText) {
                        if (lookahead.existsFrom(LookaheadMarker.ITALIC_UNDERSCORE, index + 1)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.italic = true;
                            italicUnderscore = true;
                            continue;
                        }
                    }
                    if (builder.italic && italicUnderscore) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.italic = false;
                        italicUnderscore = false;
                        continue;
                    }
                }

                //Handle Italic Asterisk
                if ((c == ASTERISK_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.italic && (builder.codeBlockContext == null) && !builder.plainText) {
                        if (!markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index) && lookahead.hasItalicAsteriskEnd(index + 1, builder.bold)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.italic = true;
                            continue;
                        }
                    }
                    if (builder.italic && !italicUnderscore) {
                        if (!markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.italic = false;
                            continue;
                        }
                    }
                }

                //Handle Strikethrough
                if ((c == TILDE_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    boolean isDoubleMarker = markdownText.startsWith(FORMATTING_CODE_STRIKETHROUGH_DOUBLE_PREFIX_SUFFIX, index);
                    if (!builder.strikethrough && (builder.codeBlockContext == null) && !builder.plainText) {
                        // Two tildes open as one double marker if a later double marker closes them, and only a double marker closes them then
                        boolean opensDouble = isDoubleMarker && lookahead.existsFrom(LookaheadMarker.STRIKETHROUGH_DOUBLE, index + 2);
                        if (isDoubleMarker && !opensDouble) {
                            // Without a later double marker, the two tildes are text like every other marker that never gets closed.
                            // As two single markers, they would open and close around nothing and vanish.
                            builder.text.append(c).append(c);
                            charsToSkip = 1;
                            continue;
                        }
                        if (opensDouble || lookahead.existsFrom(LookaheadMarker.STRIKETHROUGH, index + 1)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.strikethrough = true;
                            strikethroughDouble = opensDouble;
                            if (opensDouble) charsToSkip = 1;
                            continue;
                        }
                    }
                    if (builder.strikethrough && (!strikethroughDouble || isDoubleMarker)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.strikethrough = false;
                        if (strikethroughDouble) charsToSkip = 1;
                        strikethroughDouble = false;
                        continue;
                    }
                }

                //Handle Hyperlink Image
                if (isStartOfLine && (c == OPEN_SQUARE_BRACKETS_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (markdownText.startsWith(FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX, index)) {
                        HyperlinkImage hyperlinkImage = getHyperlinkImageFromLine(markdownText.substring(index, lineEnd));
                        if (hyperlinkImage != null) {
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlinkImage.link();
                            setImageToBuilder(builder, hyperlinkImage.imageSource(), hyperlinkImage.imageAltText());
                            lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                            setImageToBuilder(builder, null, null);
                            builder.hyperlink = null;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Image
                if (isStartOfLine && (c == EXCLAMATION_MARK_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (markdownText.startsWith(FORMATTING_CODE_IMAGE_PREFIX, index)) {
                        int labelStart = index + FORMATTING_CODE_IMAGE_PREFIX.length();
                        int labelEnd = getLinkLabelEnd(markdownText, labelStart);
                        int sourceStart = labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length();
                        int sourceEnd = (labelEnd != -1) ? getLinkTargetEnd(markdownText, sourceStart) : -1;
                        if (sourceEnd != -1) {
                            setImageToBuilder(builder, markdownText.substring(sourceStart, sourceEnd), unescape(markdownText, labelStart, labelEnd));
                            lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                            setImageToBuilder(builder, null, null);
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Hyperlink
                if ((c == OPEN_SQUARE_BRACKETS_CHAR) && (builder.hyperlink == null) && (builder.clickEvent == null) && (builder.hoverEvent == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                    int labelEnd = getLinkLabelEnd(markdownText, index + 1);
                    int targetEnd = (labelEnd != -1) ? getLinkTargetEnd(markdownText, labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length()) : -1;
                    if (targetEnd != -1) {
                        String hyperlink = markdownText.substring(labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length(), targetEnd);
                        if (isStartOfLine || !builder.text.isEmpty()) {
                            // Text that ends with a space is built already, so what is left here continues in the link without a gap
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        }
                        linkLabelEnd = labelEnd;
                        linkTargetEnd = targetEnd;
                        String clickEventId = getTextClickEventId(hyperlink);
                        if (clickEventId != null) {
                            builder.clickEvent = new MarkdownTextFragment.TextClickEvent(clickEventId);
                            continue;
                        }
                        String hoverEventId = getTextHoverEventId(hyperlink);
                        if (hoverEventId != null) {
                            builder.hoverEvent = new MarkdownTextFragment.TextHoverEvent(hoverEventId);
                            continue;
                        }
                        builder.hyperlink = new Hyperlink();
                        builder.hyperlink.link = hyperlink;
                        continue;
                    }
                }
                // The link ends at the bracket its label was found to end at when it opened, and at no other one.
                // The characters behind that bracket are exactly the target that was read back then, so skipping them never cuts into other text.
                if (index == linkLabelEnd) {
                    // The end of the link is not marked as the end of a word here. A space behind the link joins this fragment and marks it,
                    // and a line break behind the link gets a fragment of its own, like behind every other inline format.
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    charsToSkip = linkTargetEnd - index;
                    builder.hyperlink = null;
                    builder.clickEvent = null;
                    builder.hoverEvent = null;
                    linkLabelEnd = -1;
                    linkTargetEnd = -1;
                    continue;
                }

                //Handle Quote
                if (isStartOfLine && (c == GREATER_THAN_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    // The prefix opens a quote, and lines of an open quote that start with it stay part of that quote without showing it
                    if (markdownText.startsWith(FORMATTING_CODE_QUOTE_PREFIX, index)) {
                        if (builder.quoteContext == null) {
                            builder.quoteContext = new QuoteContext();
                            CalloutType calloutType = callouts ? getCalloutType(markdownText, index + FORMATTING_CODE_QUOTE_PREFIX.length(), lineEnd) : null;
                            if (calloutType != null) {
                                // The marker line shows nothing. Its empty fragment is what keeps a callout without a body in the parsed text.
                                builder.quoteContext.calloutType = calloutType;
                                builder.calloutMarker = true;
                                lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                                skipLine = true;
                                continue;
                            }
                        }
                        charsToSkip = 1;
                        continue;
                    }
                    // A line with nothing but the quote character is an empty line of the open quote
                    if ((builder.quoteContext != null) && (remainingLineLength == 1)) {
                        continue;
                    }
                }
                if (isStartOfLine && (builder.quoteContext != null)) {
                    if (containsOnly(markdownText, index, lineEnd, IS_TRIMMED_WHITESPACE)) {
                        builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        queueNewLine = true;
                        continue;
                    }
                }

                //Handle Separation Line
                if (isStartOfLine && (c == MINUS_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (markdownText.startsWith(FORMATTING_CODE_SEPARATION_LINE_PREFIX, index) && containsOnly(markdownText, index, lineEnd, IS_SEPARATION_LINE_CHARACTER)) {
                        builder.separationLine = true;
                        builder.clearText().text.append(FORMATTING_CODE_SEPARATION_LINE_PREFIX);
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        skipLine = true;
                        continue;
                    }
                }

                //Handle Code Block Single Line
                if ((c == GRAVE_ACCENT_CHAR) && !builder.plainText) {
                    if (builder.codeBlockContext == null) {
                        // A grave accent with a second one right behind it opens nothing: the second one would close the block around nothing,
                        // and both would vanish. That keeps the marker of a multi-line block that never gets closed in the text as well.
                        // The block ends at a grave accent of the same line that does not start a multi-line marker.
                        // The last grave accent of every row of grave accents is one, so any later one in the line means there is such an end.
                        if (!markdownText.startsWith(FORMATTING_CODE_CODE_BLOCK_SINGLE_LINE_EMPTY, index) && lookahead.existsBetween(LookaheadMarker.CODE_BLOCK_SINGLE_LINE, index + 1, lineEnd)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.codeBlockContext = new CodeBlockContext();
                            builder.codeBlockContext.singleLine = true;
                            continue;
                        }
                    }
                    if ((builder.codeBlockContext != null) && builder.codeBlockContext.singleLine) {
                        if (!markdownText.startsWith(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, index)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.codeBlockContext = null;
                            continue;
                        }
                    }
                }

                //Handle Code Block Multi Line
                if (isStartOfLine && (c == GRAVE_ACCENT_CHAR) && (remainingLineLength == 3) && !builder.plainText) {
                    if (builder.codeBlockContext == null) {
                        if (markdownText.startsWith(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, index) && lookahead.existsFrom(LookaheadMarker.CODE_BLOCK_MULTI_LINE_END, index + 3)) {
                            builder.codeBlockContext = new CodeBlockContext();
                            builder.codeBlockContext.singleLine = false;
                            skipLine = true;
                            continue;
                        }
                    }
                    if ((builder.codeBlockContext != null) && !builder.codeBlockContext.singleLine) {
                        if (markdownText.startsWith(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, index)) {
                            builder.codeBlockContext = null;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Plain Text Single Line
                if ((c == SEMICOLON_CHAR) && !markdownText.startsWith(FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, index)) {
                    if (!builder.plainText) {
                        if (markdownText.startsWith(FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX, index) && lookahead.existsFrom(LookaheadMarker.PLAIN_TEXT_SINGLE_LINE, index + 2)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.plainText = true;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                    if (builder.plainText) {
                        if (markdownText.startsWith(FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX, index)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.plainText = false;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                }

                //Handle Plain Text Multi Line
                if (isStartOfLine && (c == SEMICOLON_CHAR) && (remainingLineLength == 3)) {
                    if (markdownText.startsWith(FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, index)) {
                        if (!builder.plainText && lookahead.existsFrom(LookaheadMarker.PLAIN_TEXT_MULTI_LINE_END, index + 3)) {
                            builder.plainText = true;
                            skipLine = true;
                            continue;
                        }
                        if (builder.plainText) {
                            builder.plainText = false;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Alignment : Centered
                if (isStartOfLine && (c == CIRCUMFLEX_CHAR) && (remainingLineLength == 3)) {
                    if (markdownText.startsWith(FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX, index) && (builder.codeBlockContext == null)) {
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && lookahead.existsFrom(LookaheadMarker.ALIGNMENT_CENTERED_END, index + 3)) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.CENTERED;
                            skipLine = true;
                            continue;
                        }
                        if (builder.alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Alignment : Right
                if (isStartOfLine && (c == VERTICAL_BAR_CHAR) && (remainingLineLength == 3)) {
                    if (markdownText.startsWith(FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX, index) && (builder.codeBlockContext == null)) {
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && lookahead.existsFrom(LookaheadMarker.ALIGNMENT_RIGHT_END, index + 3)) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.RIGHT;
                            skipLine = true;
                            continue;
                        }
                        if (builder.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                            skipLine = true;
                            continue;
                        }
                    }
                }

            }

            if (c != NEWLINE_CHAR) {
                builder.text.append(c);
            }

            //Build fragment at every space
            if (c == SPACE_CHAR) {
                //Fix end-of-word of last fragment if needed
                if (!isStartOfLine && (lastBuiltFragment != null) && (builder.text.length() == 1)) {
                    // All spaces that directly follow would end up in the same fragment one by one, so the whole row
                    // of spaces is added here at once. This keeps long rows from rebuilding the fragment for each space.
                    int spacesEnd = index + 1;
                    while ((spacesEnd < textLength) && (markdownText.charAt(spacesEnd) == SPACE_CHAR)) spacesEnd++;
                    lastBuiltFragment.endOfWord = true;
                    lastBuiltFragment.text += ((spacesEnd - index) == 1) ? SPACE : markdownText.substring(index, spacesEnd);
                    lastBuiltFragment.updateWidth();
                    builder.clearText();
                    index = spacesEnd - 1;
                } else {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, true));
                }
                continue;
            }

            //Build fragment at end of line
            if (c == NEWLINE_CHAR) {
                lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                builder.bulletListLevel = 0;
                builder.listItemMarker = ListItemMarker.NONE;
                builder.listItemNumber = null;
                queueNewLine = true;
            }

        }

        // Whole-line constructs already handle or suppress their fragment before consuming the source line. Building
        // again at EOF would turn a terminal construct (such as a closing alignment marker) into an empty render line.
        if (!skipLine && !lineHandledByWholeLineConstruct) fragments.add(builder.build(true, true));

        return fragments;

    }

    protected static MarkdownTextFragment addFragment(List<MarkdownTextFragment> fragments, MarkdownTextFragment fragment) {
        fragments.add(fragment);
        return fragment;
    }

    protected static boolean isEscapableMarkdownCharacter(char character) {
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

    protected static boolean isCharacterEscaped(@NotNull String markdownText, int index) {
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
     * Makes the builder build image fragments with the given source and alternative text, or no image fragments if the source is NULL.
     */
    protected static void setImageToBuilder(@NotNull FragmentBuilder builder, @Nullable String imageSource, @Nullable String imageAltText) {
        builder.imageSupplier = (imageSource != null) ? ResourceSupplier.image(imageSource) : null;
        builder.imageAltText = imageAltText;
    }

    /**
     * Returns the text between the two indexes without the backslashes of its escaped characters.
     */
    @NotNull
    protected static String unescape(@NotNull String text, int start, int end) {
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
     * Returns the index of the line break that ends the line of the given index, or the text length if it is the last line.
     */
    protected static int getLineEnd(@NotNull String text, int index) {
        int lineEnd = text.indexOf(NEWLINE_CHAR, index);
        return (lineEnd != -1) ? lineEnd : text.length();
    }

    /**
     * Returns the index of the first occurrence of the character between the two indexes, or -1 if there is none.<br>
     * Other than {@link String#indexOf(int, int)}, this never reads further than the end index.
     */
    protected static int indexOf(@NotNull String text, char character, int start, int end) {
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == character) return i;
        }
        return -1;
    }

    /**
     * Returns if the text between the two indexes has no character the given check rejects, which includes having no characters at all.
     */
    protected static boolean containsOnly(@NotNull String text, int start, int end, @NotNull IntPredicate allowedCharacter) {
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
    protected static CalloutType getCalloutType(@NotNull String text, int start, int end) {
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
    protected static int getListItemMarkerEnd(@NotNull String text, int markerStart, boolean extendedBulletListMarkers, boolean numberedLists) {
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
    protected static ListItemMarker getListItemMarker(char character) {
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
    protected static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    @Nullable
    protected static HyperlinkImage getHyperlinkImageFromLine(@NotNull String line) {
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
     * Returns if the line between the two indexes is an image or an image that is a hyperlink.
     */
    protected static boolean isImageLine(@NotNull String text, int lineStart, int lineEnd) {
        if (text.startsWith(FORMATTING_CODE_IMAGE_PREFIX, lineStart)) {
            int labelEnd = getLinkLabelEnd(text, lineStart + FORMATTING_CODE_IMAGE_PREFIX.length());
            return (labelEnd != -1) && (getLinkTargetEnd(text, labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length()) != -1);
        }
        return text.startsWith(FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX, lineStart) && (getHyperlinkImageFromLine(text.substring(lineStart, lineEnd)) != null);
    }

    /**
     * An image that is a hyperlink ("[![alternative text](image source)](link)").
     */
    protected record HyperlinkImage(@NotNull String imageSource, @NotNull String imageAltText, @NotNull String link) {
    }

    @Nullable
    protected static String getTextClickEventId(@NotNull String hyperlink) {
        if (StringUtils.startsWithIgnoreCase(hyperlink, FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX)) {
            String id = StringUtils.trim(StringUtils.substring(hyperlink, FORMATTING_CODE_TEXT_CLICK_EVENT_PREFIX.length()));
            if (!id.isEmpty()) {
                return id;
            }
        }
        return null;
    }

    @Nullable
    protected static String getTextHoverEventId(@NotNull String hyperlink) {
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
    protected static String getHyperlinkFromLine(@NotNull String text, int hyperlinkStart) {
        if (!text.startsWith(OPEN_SQUARE_BRACKETS, hyperlinkStart)) return null;
        return getLinkTarget(text, hyperlinkStart + OPEN_SQUARE_BRACKETS.length());
    }

    /**
     * Returns the target of the link ("label](target)") whose label starts at the given index, or NULL if there is no valid link.
     */
    @Nullable
    protected static String getLinkTarget(@NotNull String text, int labelStart) {
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
    protected static int getLinkLabelEnd(@NotNull String text, int labelStart) {
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
    protected static int getLinkTargetEnd(@NotNull String text, int targetStart) {
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

    protected static boolean isTableSeparatorLine(@NotNull String line) {
        if (line.indexOf(VERTICAL_BAR_CHAR) == -1) return false;
        String structuralLine = splitLeadingFormattingTokens(line.trim()).content.trim();
        if (!containsUnescapedPipe(structuralLine)) {
            return false;
        }
        return hasOnlyTableSeparatorCells(structuralLine);
    }

    /**
     * Returns if the line holds nothing but table separator cells, which are rows of dashes with an optional colon at each end.
     * The cells are split by pipes, with optional whitespace around the pipes and optional pipes at both ends of the line.
     *
     * <p>This reads the line in a loop on purpose. A regular expression goes one call deeper for every cell, which overflows
     * the stack for lines with thousands of cells.</p>
     */
    protected static boolean hasOnlyTableSeparatorCells(@NotNull String line) {
        int length = line.length();
        int index = skipWhitespace(line, 0);
        if ((index < length) && (line.charAt(index) == VERTICAL_BAR_CHAR)) index = skipWhitespace(line, index + 1);
        while (true) {
            if ((index < length) && (line.charAt(index) == COLON_CHAR)) index++;
            int dashesStart = index;
            while ((index < length) && (line.charAt(index) == MINUS_CHAR)) index++;
            if (index == dashesStart) return false;
            if ((index < length) && (line.charAt(index) == COLON_CHAR)) index++;
            index = skipWhitespace(line, index);
            if (index == length) return true;
            if (line.charAt(index) != VERTICAL_BAR_CHAR) return false;
            index = skipWhitespace(line, index + 1);
            if (index == length) return true;
        }
    }

    /**
     * Returns the index of the first character at or after the given index that is no space, tab, line break, vertical tab, form feed or carriage return.
     */
    protected static int skipWhitespace(@NotNull String text, int index) {
        while (index < text.length()) {
            char c = text.charAt(index);
            if ((c != SPACE_CHAR) && ((c < '\t') || (c > '\r'))) break;
            index++;
        }
        return index;
    }
    
    protected static boolean isTableRow(@NotNull String line) {
        if (line.indexOf(VERTICAL_BAR_CHAR) == -1) return false;
        String structuralLine = splitLeadingFormattingTokens(line.trim()).content.trim();
        return containsUnescapedPipe(structuralLine) && !isTableSeparatorLine(line);
    }
    
    @NotNull
    protected static List<String> parseTableRow(@NotNull String line) {
        List<String> cells = new ArrayList<>();
        LeadingFormattingTokens leadingFormatting = splitLeadingFormattingTokens(line.trim());
        String trimmed = leadingFormatting.content.trim();
        if (trimmed.startsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> parts = splitByUnescapedPipes(trimmed);
        parts.forEach(part -> cells.add(part.trim()));
        if (!leadingFormatting.tokens.isEmpty() && !cells.isEmpty()) cells.set(0, leadingFormatting.tokens + cells.get(0));
        return cells;
    }
    
    @NotNull
    protected static List<MarkdownTextFragment.TableCell.TableCellAlignment> parseTableAlignments(@NotNull String separatorLine) {
        List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments = new ArrayList<>();
        String trimmed = splitLeadingFormattingTokens(separatorLine.trim()).content.trim();
        if (trimmed.startsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> parts = splitByUnescapedPipes(trimmed);
        for (String part : parts) {
            String cell = part.trim();
            if (cell.startsWith(COLON) && cell.endsWith(COLON)) {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.CENTER);
            } else if (cell.endsWith(COLON)) {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.RIGHT);
            } else {
                alignments.add(MarkdownTextFragment.TableCell.TableCellAlignment.LEFT);
            }
        }
        return alignments;
    }

    @NotNull
    private static LeadingFormattingTokens splitLeadingFormattingTokens(@NotNull String line) {
        int index = 0;
        while ((index + 1) < line.length()) {
            char prefix = line.charAt(index);
            ChatFormatting vanillaFormatting = prefix == ChatFormatting.PREFIX_CODE ? MinecraftFormattingState.getSectionFormattingAt(line, index) : prefix == '&' ? MinecraftFormattingState.getAmpersandFormattingAt(line, index) : null;
            TextColorFormatter customFormatting = prefix == ChatFormatting.PREFIX_CODE && vanillaFormatting == null ? MinecraftFormattingState.getCustomFormattingAt(line, index) : null;
            if ((vanillaFormatting == null) && (customFormatting == null)) break;
            index += 2;
        }
        return new LeadingFormattingTokens(line.substring(0, index), line.substring(index));
    }

    private record LeadingFormattingTokens(@NotNull String tokens, @NotNull String content) {
    }

    /**
     * A table of the text.
     *
     * @param start The index of the first character of the first line of the table.
     * @param end The index behind the last line of the table, which is the line break that ends that line or the end of the text.
     */
    protected record Table(int start, int end, @NotNull MarkdownTextFragment.TableContext tableContext) {
    }

    /**
     * Finds all tables of the text and parses their cells. The tables come in the order of their lines.
     */
    @NotNull
    protected static List<Table> findTables(@NotNull MarkdownRenderer renderer, @NotNull String markdownText) {
        // Tables need pipes
        if (markdownText.indexOf(VERTICAL_BAR_CHAR) == -1) return Collections.emptyList();

        List<Table> tables = new ArrayList<>();
        String[] lines = markdownText.split(NEWLINE);
        // Index of the current line in the text
        int lineStart = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i];
            String nextLine = (i + 1 < lines.length) ? lines[i + 1] : null;
            
            // Check if this is the start of a table
            if (isTableRow(currentLine) && nextLine != null && isTableSeparatorLine(nextLine)) {
                int tableStart = lineStart;
                // Parse the table
                MarkdownTextFragment.TableContext tableContext = new MarkdownTextFragment.TableContext();
                List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments = parseTableAlignments(nextLine);
                
                // Parse header row
                List<String> headerCells = parseTableRow(currentLine);
                MarkdownTextFragment.TableRow headerRow = new MarkdownTextFragment.TableRow(renderer);
                headerRow.isHeader = true;
                tableContext.hasHeader = true;
                
                for (int j = 0; j < headerCells.size(); j++) {
                    MarkdownTextFragment.TableCell cell = new MarkdownTextFragment.TableCell();
                    if (j < alignments.size()) {
                        cell.alignment = alignments.get(j);
                    }
                    // Parse the cell content as markdown
                    List<MarkdownTextFragment> cellFragments = parseTableCell(renderer, headerCells.get(j));
                    for (MarkdownTextFragment frag : cellFragments) {
                        frag.tableContext = tableContext;
                    }
                    cell.fragments.addAll(cellFragments);
                    headerRow.cells.add(cell);
                }
                tableContext.rows.add(headerRow);
                
                // Skip the separator line
                lineStart += currentLine.length() + nextLine.length() + 2;
                i += 2;
                
                // Parse data rows
                while (i < lines.length && isTableRow(lines[i])) {
                    List<String> dataCells = parseTableRow(lines[i]);
                    MarkdownTextFragment.TableRow dataRow = new MarkdownTextFragment.TableRow(renderer);
                    
                    for (int j = 0; j < dataCells.size(); j++) {
                        MarkdownTextFragment.TableCell cell = new MarkdownTextFragment.TableCell();
                        if (j < alignments.size()) {
                            cell.alignment = alignments.get(j);
                        }
                        // Parse the cell content as markdown
                        List<MarkdownTextFragment> cellFragments = parseTableCell(renderer, dataCells.get(j));
                        for (MarkdownTextFragment frag : cellFragments) {
                            frag.tableContext = tableContext;
                        }
                        cell.fragments.addAll(cellFragments);
                        dataRow.cells.add(cell);
                    }
                    tableContext.rows.add(dataRow);
                    lineStart += lines[i].length() + 1;
                    i++;
                }
                
                // Every line of the table was counted with a line break behind it, and the one behind its last line is no part of the table
                tables.add(new Table(tableStart, lineStart - 1, tableContext));
                i--; // Adjust because the loop will increment
            } else {
                lineStart += currentLine.length() + 1;
            }
        }
        
        return tables;
    }

    /**
     * Parses the content of a table cell.<br>
     * A cell is a single line, so it can't hold a table itself.
     */
    @NotNull
    protected static List<MarkdownTextFragment> parseTableCell(@NotNull MarkdownRenderer renderer, @NotNull String cellText) {
        return parseFragments(renderer, cellText, true, Collections.emptyList());
    }

    protected static boolean containsUnescapedPipe(@NotNull String line) {
        int length = line.length();
        for (int i = 0; i < length; i++) {
            if ((line.charAt(i) == VERTICAL_BAR_CHAR) && !isCharacterEscaped(line, i)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    protected static List<String> splitByUnescapedPipes(@NotNull String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if ((c == VERTICAL_BAR_CHAR) && !isCharacterEscaped(line, i)) {
                parts.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * The markers the parser looks ahead for to find out if something it is about to open gets closed again.
     */
    protected enum LookaheadMarker {

        FONT_SUFFIX(FORMATTING_CODE_FONT_SUFFIX, false),
        HEX_COLOR_SUFFIX(FORMATTING_CODE_HEX_COLOR_SUFFIX, false),
        BOLD(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, true),
        ITALIC_UNDERSCORE(String.valueOf(UNDERSCORE_CHAR), true),
        STRIKETHROUGH(String.valueOf(TILDE_CHAR), true),
        STRIKETHROUGH_DOUBLE(FORMATTING_CODE_STRIKETHROUGH_DOUBLE_PREFIX_SUFFIX, true),
        // A grave accent closes its block even behind a backslash, because nothing gets escaped inside the block
        CODE_BLOCK_SINGLE_LINE(String.valueOf(GRAVE_ACCENT_CHAR), false),
        PLAIN_TEXT_SINGLE_LINE(FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX, false),
        // Multi-line blocks end at a marker that starts a line, no matter what follows it in that line
        CODE_BLOCK_MULTI_LINE_END(NEWLINE + FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, false),
        PLAIN_TEXT_MULTI_LINE_END(NEWLINE + FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, false),
        ALIGNMENT_CENTERED_END(NEWLINE + FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX, false),
        ALIGNMENT_RIGHT_END(NEWLINE + FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX, false);

        protected static final int COUNT = values().length;

        @NotNull
        protected final String marker;
        /** If a backslash in front of the marker turns it into text, which makes the parser never close anything at it. */
        protected final boolean escapable;

        LookaheadMarker(@NotNull String marker, boolean escapable) {
            this.marker = marker;
            this.escapable = escapable;
        }

    }

    /**
     * Finds the next occurrence of {@link LookaheadMarker}s for the loop of the parser, which only moves forward.
     *
     * <p>A found occurrence answers every following search for its marker until the loop has moved past it, and a failed
     * search answers every following search for the rest of the text. All searches for one marker together read the text
     * only once that way, no matter how many markers without a closing one the text holds.</p>
     *
     * <p>This only works as long as the searches for a marker never start at a smaller index than an earlier one did.</p>
     *
     * <p>The parser jumps over tables, so a marker that touches a table closes nothing. A table inside a code block or plain text is text
     * of that block instead: while the parser reads the lines of such a table, the markers in those lines count like all other ones.</p>
     */
    protected static class MarkerLookahead {

        protected static final int NOT_SEARCHED = -2;
        protected static final int NOT_FOUND = -1;

        @NotNull
        protected final String text;
        /** If "*" and "~" start list items, which makes the parser skip them as the markers of those items. */
        protected final boolean extendedBulletListMarkers;
        /** The tables of the text, in the order of their lines. */
        @NotNull
        protected final List<Table> tables;
        /** The next occurrence of every marker in the whole text, tables included. */
        protected final int[] nextOccurrences = new int[LookaheadMarker.COUNT];
        /** The next occurrence of every marker that touches no table. */
        protected final int[] nextOccurrencesOutsideTables = new int[LookaheadMarker.COUNT];
        /** For every marker, the index of the first table that does not end in front of {@link #nextOccurrencesOutsideTables its next occurrence outside of tables}. */
        protected final int[] nextTables = new int[LookaheadMarker.COUNT];
        /** The index behind the table whose lines the parser reads as text, which is smaller than the index of the parser when it reads no table. */
        protected int textTableEnd = -1;
        /** Start and end index of every part of the text {@link #findSkippedRanges()} found, in ascending order. NULL until the first marker gets checked. */
        @Nullable
        protected int[] skippedRanges = null;
        protected int skippedRangeCount = 0;

        protected MarkerLookahead(@NotNull String text, boolean extendedBulletListMarkers, @NotNull List<Table> tables) {
            this.text = text;
            this.extendedBulletListMarkers = extendedBulletListMarkers;
            this.tables = tables;
            Arrays.fill(this.nextOccurrences, NOT_SEARCHED);
            Arrays.fill(this.nextOccurrencesOutsideTables, NOT_SEARCHED);
        }

        /**
         * Makes the markers in the lines of the given table count for as long as the searches start in front of the end of that table.
         */
        protected void readTableAsText(@NotNull Table table) {
            this.textTableEnd = table.end();
        }

        /**
         * Returns the index of the first occurrence of the marker at or after the given index, or -1 if there is none.<br>
         * Occurrences of markers that can be escaped do not count where {@link #isNoMarker(int)} applies or where they are the marker of a list item.
         */
        protected int indexOf(@NotNull LookaheadMarker marker, int fromIndex) {
            if (fromIndex < this.textTableEnd) {
                int nextOccurrence = this.indexOf(marker, fromIndex, this.nextOccurrences, false);
                if ((nextOccurrence != NOT_FOUND) && (nextOccurrence < this.textTableEnd)) return nextOccurrence;
                // Behind the table it reads as text, the parser jumps over tables again
                fromIndex = this.textTableEnd;
            }
            return this.indexOf(marker, fromIndex, this.nextOccurrencesOutsideTables, true);
        }

        /**
         * @param nextOccurrences Where the search remembers the occurrence it finds.
         * @param outsideTables If occurrences that touch a table do not count.
         */
        protected int indexOf(@NotNull LookaheadMarker marker, int fromIndex, int[] nextOccurrences, boolean outsideTables) {
            // The remembered occurrence is the next one behind every index a search started at before
            int nextOccurrence = nextOccurrences[marker.ordinal()];
            if ((nextOccurrence != NOT_FOUND) && (nextOccurrence < fromIndex)) {
                nextOccurrence = this.text.indexOf(marker.marker, fromIndex);
                while (nextOccurrence != NOT_FOUND) {
                    Table table = outsideTables ? this.getTableTouchedBy(marker, nextOccurrence) : null;
                    if (table != null) {
                        nextOccurrence = this.text.indexOf(marker.marker, table.end());
                    } else if (marker.escapable && (this.isNoMarker(nextOccurrence) || ((marker.marker.length() == 1) && this.isListItemMarker(nextOccurrence)))) {
                        // What the parser reads as text or skips closes nothing, so the search goes on behind it
                        nextOccurrence = this.text.indexOf(marker.marker, nextOccurrence + 1);
                    } else {
                        break;
                    }
                }
                nextOccurrences[marker.ordinal()] = nextOccurrence;
            }
            return nextOccurrence;
        }

        /**
         * Returns the table that holds at least one character of the marker that starts at the given index, or NULL if there is none.<br>
         * Like the searches for a marker, this never gets asked for a smaller index than before.
         */
        @Nullable
        protected Table getTableTouchedBy(@NotNull LookaheadMarker marker, int occurrence) {
            int nextTable = this.nextTables[marker.ordinal()];
            while ((nextTable < this.tables.size()) && (this.tables.get(nextTable).end() <= occurrence)) nextTable++;
            this.nextTables[marker.ordinal()] = nextTable;
            if (nextTable == this.tables.size()) return null;
            Table table = this.tables.get(nextTable);
            // Markers that start with a line break can start in front of a table and reach into its first line
            return ((occurrence + marker.marker.length()) > table.start()) ? table : null;
        }

        /**
         * Returns the table the given index is part of, or NULL if there is none.
         */
        @Nullable
        protected Table getTableAt(int index) {
            int low = 0;
            int high = this.tables.size() - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                Table table = this.tables.get(middle);
                if (index < table.start()) {
                    high = middle - 1;
                } else if (index >= table.end()) {
                    low = middle + 1;
                } else {
                    return table;
                }
            }
            return null;
        }

        /**
         * Returns if the marker starts at or after the given index.
         */
        protected boolean existsFrom(@NotNull LookaheadMarker marker, int fromIndex) {
            return this.indexOf(marker, fromIndex) != NOT_FOUND;
        }

        /**
         * Returns if italic text that a single asterisk opens gets closed by an asterisk at or after the given index.
         *
         * <p>Italic text ends at an asterisk that has no second one behind it, but the parser reads asterisks in pairs as markers of bold text first.
         * This goes through the rows of asterisks the way the parser will: the pairs of a row open and close bold text, and only an asterisk
         * that is left over ends the italic text. A pair that finds no later pair to close its bold text opens none, which leaves its second asterisk over.</p>
         *
         * <p>Italic text that opens inside of bold text has to end inside of it as well, so the pair that closes that bold text means there is no end.</p>
         *
         * @param boldOpen If the italic text opens inside of bold text.
         */
        protected boolean hasItalicAsteriskEnd(int fromIndex, boolean boldOpen) {
            boolean insideOuterBold = boldOpen;
            int length = this.text.length();
            int index = fromIndex;
            while (true) {
                int rowStart = this.text.indexOf(ASTERISK_CHAR, index);
                if (rowStart == NOT_FOUND) break;
                if (rowStart >= this.textTableEnd) {
                    // The parser jumps over all tables but the one it reads as text
                    Table table = this.getTableAt(rowStart);
                    if (table != null) {
                        index = table.end();
                        continue;
                    }
                }
                index = rowStart + 1;
                // An asterisk that is no marker or the marker of a list item is no part of a row. The row starts behind it then.
                if (this.isNoMarker(rowStart) || this.isListItemMarker(rowStart)) continue;
                while ((index < length) && (this.text.charAt(index) == ASTERISK_CHAR)) index++;
                int rowLength = index - rowStart;
                if ((rowLength % 2) == 1) return true;
                for (int pair = 0; pair < (rowLength / 2); pair++) {
                    if (boldOpen && insideOuterBold) return false;
                    boldOpen = !boldOpen;
                }
            }
            // Bold text that is still open here was opened by a pair without a later pair, so it is none and the second asterisk of that pair is the end
            return boldOpen && !insideOuterBold;
        }

        /**
         * Returns if a marker at the given index closes nothing, because the parser reads it as text or never reaches it:
         * it is escaped, or it is part of the target of a link or image, or of a line that is an image.
         */
        protected boolean isNoMarker(int index) {
            if (isCharacterEscaped(this.text, index)) return true;
            if (this.skippedRanges == null) this.findSkippedRanges();
            int low = 0;
            int high = (this.skippedRangeCount / 2) - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (index < this.skippedRanges[middle * 2]) {
                    high = middle - 1;
                } else if (index >= this.skippedRanges[(middle * 2) + 1]) {
                    low = middle + 1;
                } else {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns if the character at the given index looks like the marker of a list item that an asterisk or a tilde starts,
         * which makes the parser skip it instead of closing a format with it.<br>
         * Like {@link #findSkippedRanges()}, this only looks at the text, and is wrong in the same harmless way if the parser reads the line as text.
         */
        protected boolean isListItemMarker(int index) {
            if (!this.extendedBulletListMarkers || (getListItemMarkerEnd(this.text, index, true, false) == -1)) return false;
            // The marker follows the indent of its item, which is nothing but spaces
            int indentStart = index;
            while ((indentStart > 0) && (this.text.charAt(indentStart - 1) == SPACE_CHAR) && ((index - indentStart) < MAX_LIST_ITEM_INDENT)) indentStart--;
            return ((index - indentStart) <= MAX_LIST_ITEM_INDENT) && ((indentStart == 0) || (this.text.charAt(indentStart - 1) == NEWLINE_CHAR));
        }

        /**
         * Finds all parts of the text that look like something the parser skips: targets of links and images, and whole lines that are images.<br>
         * This only looks at the text and not at what the parser makes of the text around it, so it can take a part for skipped that the parser reads.
         * A marker in such a part then is no end for a marker in front of it, which stays text for that reason.
         * That is the harmless way to be wrong: the other way, a format never gets closed and runs to the end of the text.
         */
        protected void findSkippedRanges() {
            String text = this.text;
            int[] ranges = new int[16];
            int count = 0;
            int lineStart = 0;
            int lineEnd = getLineEnd(text, 0);
            boolean imageLine = isImageLine(text, lineStart, lineEnd);
            int targetOpening = text.indexOf(FORMATTING_CODE_HYPERLINK_INNER_PREFIX);
            while (targetOpening != -1) {
                while (lineEnd < targetOpening) {
                    lineStart = lineEnd + 1;
                    lineEnd = getLineEnd(text, lineStart);
                    imageLine = isImageLine(text, lineStart, lineEnd);
                }
                int rangeStart = imageLine ? lineStart : (targetOpening + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length());
                // No link ends at an escaped bracket, so there is no target behind one
                int rangeEnd = imageLine ? lineEnd : (isCharacterEscaped(text, targetOpening) ? -1 : getLinkTargetEnd(text, rangeStart));
                if (rangeEnd != -1) {
                    if (count == ranges.length) ranges = Arrays.copyOf(ranges, count * 2);
                    ranges[count++] = rangeStart;
                    ranges[count++] = rangeEnd;
                }
                targetOpening = text.indexOf(FORMATTING_CODE_HYPERLINK_INNER_PREFIX, (rangeEnd != -1) ? rangeEnd : rangeStart);
            }
            this.skippedRanges = ranges;
            this.skippedRangeCount = count;
        }

        /**
         * Returns if the marker starts at or after the given index and before the end index.
         */
        protected boolean existsBetween(@NotNull LookaheadMarker marker, int fromIndex, int endIndex) {
            int nextOccurrence = this.indexOf(marker, fromIndex);
            return (nextOccurrence != NOT_FOUND) && (nextOccurrence < endIndex);
        }

    }

    protected static class FragmentBuilder {

        protected final MarkdownRenderer renderer;
        protected final StringBuilder text = new StringBuilder();
        protected DrawableColor textColor = null;
        protected ResourceSupplier<ITexture> imageSupplier = null;
        protected String imageAltText = null;
        protected boolean separationLine = false;
        protected boolean bold = false;
        protected boolean italic = false;
        protected boolean strikethrough = false;
        @NotNull
        protected MinecraftFormattingState minecraftFormatting = MinecraftFormattingState.EMPTY;
        protected boolean bulletListItemStart = false;
        protected int bulletListLevel = 0;
        @NotNull
        protected ListItemMarker listItemMarker = ListItemMarker.NONE;
        protected String listItemNumber = null;
        @NotNull
        protected MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
        protected Hyperlink hyperlink = null;
        protected MarkdownTextFragment.TextClickEvent clickEvent = null;
        protected MarkdownTextFragment.TextHoverEvent hoverEvent = null;
        @NotNull
        protected HeadlineType headlineType = HeadlineType.NONE;
        protected QuoteContext quoteContext = null;
        protected boolean calloutMarker = false;
        protected CodeBlockContext codeBlockContext = null;
        protected ResourceLocation font = null;
        protected boolean plainText = false;
        protected MarkdownTextFragment.TableContext tableContext = null;

        protected FragmentBuilder(MarkdownRenderer renderer) {
            this.renderer = renderer;
        }

        @NotNull
        protected MarkdownTextFragment build(boolean naturalLineBreakAfter, boolean endOfWord) {
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, this.text.toString());
            frag.font = font;
            frag.textColor = textColor;
            frag.imageSupplier = imageSupplier;
            frag.imageAltText = imageAltText;
            frag.separationLine = separationLine;
            if (separationLine) {
                frag.unscaledTextHeight = 1;
            }
            frag.bold = bold;
            frag.italic = italic;
            frag.strikethrough = strikethrough;
            frag.minecraftFormatting = minecraftFormatting;
            frag.bulletListLevel = bulletListLevel;
            frag.listItemMarker = listItemMarker;
            frag.listItemNumber = listItemNumber;
            frag.bulletListItemStart = bulletListItemStart;
            bulletListItemStart = false;
            frag.alignment = alignment;
            frag.hyperlink = hyperlink;
            if (hyperlink != null) {
                hyperlink.hyperlinkFragments.add(frag);
            }
            frag.clickEvent = clickEvent;
            if (clickEvent != null) {
                clickEvent.eventFragments.add(frag);
            }
            frag.hoverEvent = hoverEvent;
            if (hoverEvent != null) {
                hoverEvent.eventFragments.add(frag);
            }
            frag.headlineType = headlineType;
            frag.quoteContext = quoteContext;
            frag.calloutMarker = calloutMarker;
            calloutMarker = false;
            if (quoteContext != null) {
                quoteContext.quoteFragments.add(frag);
            }
            frag.plainText = this.plainText;
            if (!this.plainText) {
                frag.codeBlockContext = codeBlockContext;
                if (codeBlockContext != null) {
                    codeBlockContext.codeBlockFragments.add(frag);
                }
            }
            frag.tableContext = tableContext;
            frag.naturalLineBreakAfter = naturalLineBreakAfter;
            frag.endOfWord = endOfWord;
            frag.updateWidth();
            this.clearText();
            return frag;
        }

        protected FragmentBuilder clearText() {
            this.text.setLength(0);
            return this;
        }

    }

}
