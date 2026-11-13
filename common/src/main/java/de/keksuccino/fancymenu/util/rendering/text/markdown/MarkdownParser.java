package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownFormatMarkers.FormatMarker;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTableParser.Table;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CodeBlockContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.ListItemMarker;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownSyntax.*;

/**
 * Turns the text of a {@link MarkdownRenderer} into the fragments the renderer shows.
 *
 * <p>The work is split between a few classes. {@link MarkdownTableParser} finds the tables of the text. {@link MarkdownStructure} finds everything else
 * that decides how each character has to be read, and collects the {@link MarkdownFormatMarkers} of the text. This class builds the fragments from all of that.
 * {@link MarkdownSyntax} holds the characters and markers of the syntax, and the checks the other classes share.</p>
 */
public class MarkdownParser {

    private static final MarkdownRenderer.MarkdownLineAlignment[] LINE_ALIGNMENTS = MarkdownRenderer.MarkdownLineAlignment.values();

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
            tables = MarkdownTableParser.findTables(renderer, markdownText);
        }

        return parseFragments(renderer, markdownText, parseMarkdown, tables);

    }

    /**
     * Parses text that has its tables found already.
     *
     * <p>The text gets read twice. {@link MarkdownStructure} reads it first and finds everything that decides how the loop here has to read
     * each character. The loop then builds the fragments from that, and decides on its own only about the formats that need a marker
     * to close them: bold, italic and struck through text, fonts and colors. It opens one of them only if {@link MarkdownFormatMarkers}
     * knows a marker behind it that the loop will read as a marker, so no format stays open because its closing marker turned out to be text.</p>
     *
     * <p>Both read the text once and never copy the rest of it. Everything that looks ahead is either limited to
     * the current line, stops at the first character that settles the answer, or goes through the lookahead of {@link MarkdownStructure} or {@link MarkdownFormatMarkers}.
     * This keeps the parsing time linear to the text length, so every new lookahead has to follow the same rules.</p>
     *
     * @param tables The tables of the text, in the order of their lines.
     */
    @NotNull
    protected static List<MarkdownTextFragment> parseFragments(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, boolean parseMarkdown, @NotNull List<Table> tables) {

        List<MarkdownTextFragment> fragments = new ArrayList<>();

        FragmentBuilder builder = new FragmentBuilder(renderer);
        MarkdownStructure structure = new MarkdownStructure(markdownText, parseMarkdown, tables, renderer.isExtendedBulletListMarkersEnabled(), renderer.isNumberedListsEnabled(), renderer.isCalloutsEnabled());
        MarkdownFormatMarkers formatMarkers = structure.getFormatMarkers();
        boolean queueNewLine = true;
        boolean italicUnderscore = false;
        boolean strikethroughDouble = false;
        //Index behind the marker that opened bold text last
        int boldMarkerEnd = -1;
        boolean lineHandledByWholeLineConstruct = false;
        //The fragment that a space joins when the text of the fragment is what the space follows, which ends the word of that fragment.
        //Markers that open something and the prefixes of lines set it to NULL, so a space behind them becomes a fragment of what they opened.
        MarkdownTextFragment lastBuiltFragment = null;

        int textLength = markdownText.length();
        //Index of the line break that ends a line, or the text length for the last line. It only gets brought up to the line of the current index where the loop needs it.
        int lineEnd = -1;

        for (int index = 0; index < textLength; index++) {

            char c = markdownText.charAt(index);

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;
            if (isStartOfLine) lineHandledByWholeLineConstruct = false;

            MarkdownStructure.Event event = structure.getEventAt(index);

            if (event != MarkdownStructure.Event.NONE) {
                switch (event) {
                    case TABLE: {
                        Table table = tables.get(structure.getEventValue());
                        MarkdownTextFragment tableFragment = new MarkdownTextFragment(renderer, "[TABLE]");
                        tableFragment.table = true;
                        tableFragment.tableContext = table.tableContext();
                        tableFragment.naturalLineBreakAfter = true;
                        // Inherit current alignment from builder
                        tableFragment.alignment = builder.alignment;
                        lastBuiltFragment = addFragment(fragments, tableFragment);
                        break;
                    }
                    case SKIPPED_LINE_BREAK:
                    case QUOTE_EMPTY_LINE:
                        break;
                    case FORMATTING_CODE: {
                        // The formatting gets applied before Markdown, so its state survives the word-sized fragments built below
                        ChatFormatting vanillaFormatting = (c == ChatFormatting.PREFIX_CODE) ? MinecraftFormattingState.getSectionFormattingAt(markdownText, index) : MinecraftFormattingState.getAmpersandFormattingAt(markdownText, index);
                        if (!builder.text.isEmpty()) fragments.add(builder.build(false, false));
                        builder.minecraftFormatting = (vanillaFormatting != null) ? builder.minecraftFormatting.apply(vanillaFormatting) : builder.minecraftFormatting.apply(Objects.requireNonNull(MinecraftFormattingState.getCustomFormattingAt(markdownText, index)));
                        // A following isolated space must be built with the new formatting state instead of being folded
                        // into the fragment before this formatting boundary by the whitespace optimization below.
                        lastBuiltFragment = null;
                        // Skips the character of the code. A code at the start of a line leaves that start to what follows it.
                        index++;
                        queueNewLine = isStartOfLine;
                        break;
                    }
                    case ESCAPE:
                        builder.text.append(markdownText.charAt(index + 1));
                        index++;
                        break;
                    case HEADLINE: {
                        int hashtags = structure.getEventValue();
                        builder.headlineType = (hashtags == 1) ? HeadlineType.BIGGEST : ((hashtags == 2) ? HeadlineType.BIGGER : HeadlineType.BIG);
                        lastBuiltFragment = null;
                        // Skips the hashtags behind this one and the space
                        index += hashtags;
                        break;
                    }
                    case LIST_ITEM: {
                        // The marker of an item follows 2 spaces for every level below the first one
                        int markerStart = structure.getEventValue();
                        int markerEnd = structure.getSecondEventValue();
                        builder.bulletListLevel = ((markerStart - index) / 2) + 1;
                        builder.bulletListItemStart = true;
                        builder.listItemMarker = getListItemMarker(markdownText.charAt(markerStart));
                        builder.listItemNumber = (builder.listItemMarker == ListItemMarker.NUMBER) ? markdownText.substring(markerStart, markerEnd) : null;
                        lastBuiltFragment = null;
                        // Skips the indent, the marker and the space behind it
                        index = markerEnd;
                        break;
                    }
                    case HYPERLINK_IMAGE: {
                        HyperlinkImage hyperlinkImage = structure.getHyperlinkImage(structure.getEventValue());
                        builder.hyperlink = new Hyperlink();
                        builder.hyperlink.link = hyperlinkImage.link();
                        setImageToBuilder(builder, hyperlinkImage.imageSource(), hyperlinkImage.imageAltText());
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        setImageToBuilder(builder, null, null);
                        builder.hyperlink = null;
                        break;
                    }
                    case IMAGE: {
                        int labelStart = index + FORMATTING_CODE_IMAGE_PREFIX.length();
                        int labelEnd = structure.getEventValue();
                        int sourceEnd = structure.getSecondEventValue();
                        setImageToBuilder(builder, markdownText.substring(labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length(), sourceEnd), unescape(markdownText, labelStart, labelEnd));
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        setImageToBuilder(builder, null, null);
                        break;
                    }
                    case LINK_OPEN: {
                        String hyperlink = markdownText.substring(structure.getEventValue() + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length(), structure.getSecondEventValue());
                        // Text that ends with a space is built already, so what is left here continues in the link without a gap
                        buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                        lastBuiltFragment = null;
                        String clickEventId = getTextClickEventId(hyperlink);
                        String hoverEventId = (clickEventId == null) ? getTextHoverEventId(hyperlink) : null;
                        if (clickEventId != null) {
                            builder.clickEvent = new MarkdownTextFragment.TextClickEvent(clickEventId);
                        } else if (hoverEventId != null) {
                            builder.hoverEvent = new MarkdownTextFragment.TextHoverEvent(hoverEventId);
                        } else {
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlink;
                        }
                        break;
                    }
                    case LINK_CLOSE:
                        // The end of the link is not marked as the end of a word here. A space behind the link joins this fragment and marks it,
                        // and a line break behind the link gets a fragment of its own, like behind every other inline format.
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.hyperlink = null;
                        builder.clickEvent = null;
                        builder.hoverEvent = null;
                        // Skips the target of the link
                        index = structure.getEventValue();
                        break;
                    case QUOTE_OPEN:
                        builder.quoteContext = new QuoteContext();
                        lastBuiltFragment = null;
                        // Skips the space of the prefix
                        index++;
                        break;
                    case QUOTE_PREFIX:
                        lastBuiltFragment = null;
                        index++;
                        break;
                    case CALLOUT:
                        // The marker line shows nothing. Its empty fragment is what keeps a callout without a body in the parsed text.
                        builder.quoteContext = new QuoteContext();
                        builder.quoteContext.calloutType = CALLOUT_TYPES[structure.getEventValue()];
                        builder.calloutMarker = true;
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        break;
                    case QUOTE_END:
                        builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        queueNewLine = true;
                        break;
                    case SEPARATION_LINE:
                        builder.separationLine = true;
                        builder.clearText().text.append(FORMATTING_CODE_SEPARATION_LINE_PREFIX);
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        break;
                    case CODE_OPEN:
                        buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                        lastBuiltFragment = null;
                        builder.codeBlockContext = new CodeBlockContext();
                        builder.codeBlockContext.singleLine = true;
                        break;
                    case CODE_CLOSE:
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.codeBlockContext = null;
                        break;
                    case CODE_BLOCK_OPEN:
                        builder.codeBlockContext = new CodeBlockContext();
                        builder.codeBlockContext.singleLine = false;
                        break;
                    case CODE_BLOCK_CLOSE:
                        builder.codeBlockContext = null;
                        break;
                    case PLAIN_TEXT_OPEN:
                        buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                        lastBuiltFragment = null;
                        builder.plainText = true;
                        // Skips the second character of the marker
                        index++;
                        break;
                    case PLAIN_TEXT_CLOSE:
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.plainText = false;
                        index++;
                        break;
                    case PLAIN_TEXT_BLOCK_OPEN:
                        builder.plainText = true;
                        break;
                    case PLAIN_TEXT_BLOCK_CLOSE:
                        builder.plainText = false;
                        break;
                    case ALIGNMENT:
                        builder.alignment = LINE_ALIGNMENTS[structure.getEventValue()];
                        break;
                    default:
                        break;
                }
                if (event.wholeLine) {
                    // Nothing else of the line gets read, and the line break that ends it builds no fragment
                    lineHandledByWholeLineConstruct = true;
                    builder.headlineType = HeadlineType.NONE;
                    builder.separationLine = false;
                    queueNewLine = true;
                    if (lineEnd < index) lineEnd = getLineEnd(markdownText, index);
                    // The lines of a table count as one line, which ends at the line break behind the last one
                    index = (event == MarkdownStructure.Event.TABLE) ? tables.get(structure.getEventValue()).end() : lineEnd;
                }
                continue;
            }

            // Inside of code and plain text, the markers of the formats are text
            if (parseMarkdown && (builder.codeBlockContext == null) && !builder.plainText) {

                //Handle Fonts
                if (c == PERCENTAGE_CHAR) {
                    if (builder.font == null) {
                        if (lineEnd < index) lineEnd = getLineEnd(markdownText, index);
                        int fontNameEnd = getFontPrefixEnd(markdownText, index, lineEnd);
                        // The suffix has to start behind the prefix. The last character of the prefix can start something that looks like a suffix, which closes nothing.
                        if ((fontNameEnd != -1) && formatMarkers.existsFrom(FormatMarker.FONT_SUFFIX, fontNameEnd + 1)) {
                            ResourceLocation font = null;
                            try {
                                font = ResourceLocation.parse(markdownText.substring(index + FORMATTING_CODE_FONT_PREFIX.length(), fontNameEnd));
                            } catch (Exception ignore) {}
                            if (font != null) {
                                buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                                lastBuiltFragment = null;
                                builder.font = font;
                                index = fontNameEnd;
                                continue;
                            }
                        }
                    }
                    if (markdownText.startsWith(FORMATTING_CODE_FONT_SUFFIX, index) && (builder.font != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.font = null;
                        index += 3;
                        continue;
                    }
                }

                //Handle Color Formatting
                if (c == PERCENTAGE_CHAR) {
                    if (markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_PREFIX, index) && !markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_SUFFIX, index) && (builder.textColor == null)) {
                        int colorStart = index + FORMATTING_CODE_HEX_COLOR_PREFIX.length();
                        if (lineEnd < index) lineEnd = getLineEnd(markdownText, index);
                        int colorEnd = indexOf(markdownText, PERCENTAGE_CHAR, colorStart, lineEnd);
                        // Like the suffix of a font, the suffix has to start behind the prefix
                        if ((colorEnd != -1) && formatMarkers.existsFrom(FormatMarker.COLOR_SUFFIX, colorEnd + 1)) {
                            DrawableColor color = DrawableColor.ofHtml(markdownText.substring(colorStart, colorEnd));
                            if (color != DrawableColor.EMPTY) {
                                buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                                lastBuiltFragment = null;
                                builder.textColor = color;
                                index = colorEnd;
                                continue;
                            }
                        }
                    }
                    if (markdownText.startsWith(FORMATTING_CODE_HEX_COLOR_SUFFIX, index) && (builder.textColor != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.textColor = null;
                        index += 2;
                        continue;
                    }
                }

                //Handle Bold
                if (c == ASTERISK_CHAR) {
                    if (!builder.bold) {
                        if (markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index) && formatMarkers.existsFrom(FormatMarker.ASTERISK_PAIR, index + 2)) {
                            buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                            lastBuiltFragment = null;
                            builder.bold = true;
                            index++;
                            boldMarkerEnd = index + 1;
                            continue;
                        }
                    }
                    if (builder.bold) {
                        if (markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.bold = false;
                            index++;
                            continue;
                        }
                    }
                }

                //Handle Italic Underscore
                if ((c == UNDERSCORE_CHAR) && formatMarkers.isAt(FormatMarker.UNDERSCORE, index)) {
                    if (!builder.italic) {
                        if (formatMarkers.existsFrom(FormatMarker.UNDERSCORE, index + 1)) {
                            buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                            lastBuiltFragment = null;
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
                if (c == ASTERISK_CHAR) {
                    if (!builder.italic) {
                        // Behind the marker that opened bold text, the asterisk is part of the same row of asterisks. Three asterisks open bold and italic text
                        // together, and either of them can be the first one to close again. Everywhere else, italic text that opens inside of bold text ends inside of it.
                        if (!markdownText.startsWith(FORMATTING_CODE_BOLD_PREFIX_SUFFIX, index) && formatMarkers.hasItalicAsteriskEnd(index + 1, builder.bold, builder.bold && (boldMarkerEnd != index))) {
                            buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                            lastBuiltFragment = null;
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
                if (c == TILDE_CHAR) {
                    boolean isDoubleMarker = markdownText.startsWith(FORMATTING_CODE_STRIKETHROUGH_DOUBLE_PREFIX_SUFFIX, index);
                    if (!builder.strikethrough) {
                        // Two tildes open as one double marker if a later double marker closes them, and only a double marker closes them then
                        boolean opensDouble = isDoubleMarker && formatMarkers.existsFrom(FormatMarker.TILDE_PAIR, index + 2);
                        if (isDoubleMarker && !opensDouble) {
                            // Without a later double marker, the two tildes are text like every other marker that never gets closed.
                            // As two single markers, they would open and close around nothing and vanish.
                            builder.text.append(c).append(c);
                            index++;
                            continue;
                        }
                        if (opensDouble || formatMarkers.existsFrom(FormatMarker.TILDE, index + 1)) {
                            buildTextInFrontOfOpeningMarker(fragments, builder, isStartOfLine);
                            lastBuiltFragment = null;
                            builder.strikethrough = true;
                            strikethroughDouble = opensDouble;
                            if (opensDouble) index++;
                            continue;
                        }
                    }
                    if (builder.strikethrough && (!strikethroughDouble || isDoubleMarker)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.strikethrough = false;
                        if (strikethroughDouble) index++;
                        strikethroughDouble = false;
                        continue;
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
        if (!lineHandledByWholeLineConstruct) fragments.add(builder.build(true, true));

        return fragments;

    }

    /**
     * Builds the text in front of a marker that opens something, so the text behind the marker starts a new fragment.<br>
     * A marker at the start of a line gets a fragment in front of it as well, which has no text then.
     */
    protected static void buildTextInFrontOfOpeningMarker(@NotNull List<MarkdownTextFragment> fragments, @NotNull FragmentBuilder builder, boolean isStartOfLine) {
        if (isStartOfLine || !builder.text.isEmpty()) fragments.add(builder.build(false, false));
    }

    protected static MarkdownTextFragment addFragment(List<MarkdownTextFragment> fragments, MarkdownTextFragment fragment) {
        fragments.add(fragment);
        return fragment;
    }

    /**
     * Makes the builder build image fragments with the given source and alternative text, or no image fragments if the source is NULL.
     */
    protected static void setImageToBuilder(@NotNull FragmentBuilder builder, @Nullable String imageSource, @Nullable String imageAltText) {
        builder.imageSupplier = (imageSource != null) ? ResourceSupplier.image(imageSource) : null;
        builder.imageAltText = imageAltText;
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
