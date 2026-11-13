package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownFormatMarkers.FormatMarker;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTableParser.Table;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CalloutType;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownSyntax.*;

/**
 * The structure of a Markdown text: where its formatting codes, escaped characters, headlines, list items, images, links, quotes,
 * separation lines, code, plain text, aligned blocks and tables are.
 *
 * <p>The structure gets found in one pass over the text, before the parser builds fragments. It holds everything that depends on nothing
 * but the text in front of it and on the markers {@link MarkerLookahead} finds behind it. The parser asks for the {@link Event} of
 * every character it reads, and decides on its own only about the formats that need a marker to close them.</p>
 *
 * <p>The markers of those formats close nothing where the structure makes the parser read them as text or skip them. Only the pass
 * knows where that is, so it collects all other ones as {@link MarkdownFormatMarkers} for the parser.</p>
 *
 * <p>The pass jumps over everything its events make the parser skip, like the target of a link or the rest of a line. What the parser
 * skips on its own are the prefix of a font or color it opens, and the rest of a marker that opens or closes a format.
 * None of these can hold a character that starts an event, so the pass reads them like text. It only jumps over the names of fonts,
 * whose underscores are no format markers. Whatever else the parser skips on its own has to keep it that way, or it has to become an event.</p>
 */
final class MarkdownStructure {

    /**
     * TRUE for every character that can be an event or a format marker, or change what the pass knows about the text, when it is not the first character of a line.<br>
     * The pass reads all other characters only at the start of a line, so a character that is missing here never becomes one of these inside of a line.
     */
    private static final boolean[] INSIDE_LINE_EVENT_CHARACTERS = new boolean[ChatFormatting.PREFIX_CODE + 1];
    static {
        for (char character : new char[] {NEWLINE_CHAR, ChatFormatting.PREFIX_CODE, AMPERSAND_CHAR, BACKSLASH_CHAR, OPEN_SQUARE_BRACKETS_CHAR, CLOSE_SQUARE_BRACKETS_CHAR, GRAVE_ACCENT_CHAR, SEMICOLON_CHAR, ASTERISK_CHAR, UNDERSCORE_CHAR, TILDE_CHAR, PERCENTAGE_CHAR}) {
            INSIDE_LINE_EVENT_CHARACTERS[character] = true;
        }
    }

    /** The index of the bracket that ends the label of the open link while no link is open. */
    private static final int NO_LINK = -1;
    /** The index of the bracket that ends the label of the open link while that bracket is not found, because the one found before is text of code or of plain text. */
    private static final int LINK_LABEL_END_UNKNOWN = -2;

    /** Every event takes this many values in {@link #events}: its index in the text, the ordinal of its {@link Event} and its two values. */
    private static final int EVENT_SIZE = 4;
    private static final Event[] EVENT_TYPES = Event.values();
    private static final int[] NO_EVENTS = new int[0];

    private int[] events = NO_EVENTS;
    /** Index behind the last value in {@link #events}. */
    private int eventsEnd = 0;
    /** Index in {@link #events} of the first event the parser has not read. */
    private int nextEvent = 0;
    /** Index in the text of the first event the parser has not read, or the highest integer if it has read all of them. */
    private int nextEventIndex = Integer.MAX_VALUE;
    private int eventValue;
    private int secondEventValue;
    /** The images of the {@link Event#HYPERLINK_IMAGE} events. NULL until the first one gets found. */
    @Nullable
    private List<HyperlinkImage> hyperlinkImages = null;
    final MarkdownFormatMarkers formatMarkers = new MarkdownFormatMarkers();

    /**
     * Reads the text once and finds all of its events and format markers.
     *
     * @param parseMarkdown If the text is Markdown. Other text only has formatting codes.
     * @param tables The tables of the text, in the order of their lines.
     */
    MarkdownStructure(@NotNull String text, boolean parseMarkdown, @NotNull List<Table> tables, boolean extendedBulletListMarkers, boolean numberedLists, boolean callouts) {

        MarkerLookahead lookahead = new MarkerLookahead(text, tables);
        MarkdownFormatMarkers formatMarkers = this.formatMarkers;
        int length = text.length();
        boolean queueNewLine = true;
        boolean headlineLine = false;
        boolean code = false;
        boolean singleLineCode = false;
        boolean plainText = false;
        boolean quote = false;
        int centeredAlignmentMarkers = 0;
        int rightAlignmentMarkers = 0;
        //Index of the last underscore that is no part of an event, code or plain text
        int previousUnderscore = Integer.MIN_VALUE;
        //Index of the square bracket that ends the label of the open link, and of the round bracket that ends its target
        int linkLabelEnd = NO_LINK;
        int linkTargetEnd = -1;
        //Where the event that opened the open link is in the events
        int linkOpenEvent = -1;
        //Index of the line break that ends the line of the current index, or the text length if it is the last line
        int lineEnd = -1;
        //The tables follow the order of their lines, so the loop only ever has to look out for the next one
        int nextTable = 0;
        int nextTableStart = tables.isEmpty() ? -1 : tables.get(0).start();

        for (int index = 0; index < length; index++) {

            char c = text.charAt(index);

            boolean isStartOfLine = queueNewLine;
            // Most characters are somewhere inside of a line and start nothing there
            if (!isStartOfLine && ((c >= INSIDE_LINE_EVENT_CHARACTERS.length) || !INSIDE_LINE_EVENT_CHARACTERS[c])) continue;
            queueNewLine = false;

            if (lineEnd < index) lineEnd = getLineEnd(text, index);

            // A label stays in its line, so a link whose label has no end in this line is none
            if ((c == NEWLINE_CHAR) && (linkLabelEnd == LINK_LABEL_END_UNKNOWN)) {
                this.removeEvent(linkOpenEvent);
                linkLabelEnd = NO_LINK;
            }

            // Check for a table
            if (index == nextTableStart) {
                Table table = tables.get(nextTable);
                nextTable++;
                nextTableStart = (nextTable < tables.size()) ? tables.get(nextTable).start() : -1;
                if (code || plainText) {
                    // Inside a code block or plain text, the lines of the table are text like every other line of it
                    lookahead.readTableAsText(table);
                } else {
                    this.addEvent(index, Event.TABLE, nextTable - 1, 0);
                    index = table.end();
                    queueNewLine = true;
                    continue;
                }
            }

            //Skip empty line under headline
            if ((c == NEWLINE_CHAR) && headlineLine) {
                headlineLine = false;
                int nextLineStart = index + 1;
                if (containsOnly(text, nextLineStart, getLineEnd(text, nextLineStart), IS_SPACE)) {
                    // The line behind this line break does not count as started, so nothing in it gets read as the start of a line
                    this.addEvent(index, Event.SKIPPED_LINE_BREAK, 0, 0);
                    continue;
                }
            }

            // Formatting codes come before Markdown. Ampersand codes are found here, behind the targets of links that got skipped, so URL query parameters stay intact.
            if (((c == ChatFormatting.PREFIX_CODE) || (c == AMPERSAND_CHAR)) && isFormattingCode(text, index)) {
                this.addEvent(index, Event.FORMATTING_CODE, 0, 0);
                index++;
                queueNewLine = isStartOfLine;
                continue;
            }

            if (parseMarkdown) {

                boolean markdown = !code && !plainText;

                //Handle Escapes
                if ((c == BACKSLASH_CHAR) && markdown && ((index + 1) < length) && isEscapableMarkdownCharacter(text.charAt(index + 1))) {
                    this.addEvent(index, Event.ESCAPE, 0, 0);
                    index++;
                    continue;
                }

                //Handle Headline
                if (isStartOfLine && (c == HASHTAG_CHAR) && markdown) {
                    // The prefix has one hashtag for the biggest headline and one more for every smaller one
                    int hashtags = text.startsWith(HEADLINE_PREFIX_BIGGEST, index) ? 1 : (text.startsWith(HEADLINE_PREFIX_BIGGER, index) ? 2 : (text.startsWith(HEADLINE_PREFIX_BIG, index) ? 3 : 0));
                    if (hashtags > 0) {
                        this.addEvent(index, Event.HEADLINE, hashtags, 0);
                        headlineLine = true;
                        // Skips the hashtags behind this one and the space
                        index += hashtags;
                        continue;
                    }
                }

                // Handle List Items (all levels)
                // Markers that are format markers as well start a list item first
                if (isStartOfLine && markdown) {
                    // The marker of an item follows 2 spaces for every level below the first one
                    int markerStart = index;
                    while ((markerStart < lineEnd) && (text.charAt(markerStart) == SPACE_CHAR)) markerStart++;
                    int markerEnd = getListItemMarkerEnd(text, markerStart, extendedBulletListMarkers, numberedLists);
                    if ((markerEnd != -1) && ((markerStart - index) <= MAX_LIST_ITEM_INDENT)) {
                        this.addEvent(index, Event.LIST_ITEM, markerStart, markerEnd);
                        index = markerEnd;
                        continue;
                    }
                }

                // Format markers. The pass gets here for exactly the characters the parser reads as text or as a format marker:
                // everything else is escaped, part of code or plain text, or belongs to an event the parser takes it for.
                if (markdown) {
                    switch (c) {
                        case ASTERISK_CHAR:
                            formatMarkers.addPaired(FormatMarker.ASTERISK, FormatMarker.ASTERISK_PAIR, index);
                            continue;
                        case UNDERSCORE_CHAR:
                            // Underscores in a row are text. The one behind this one gets read the same way this one does, so looking at the text is enough for it.
                            if ((previousUnderscore != (index - 1)) && ((index + 1 == length) || (text.charAt(index + 1) != UNDERSCORE_CHAR))) formatMarkers.add(FormatMarker.UNDERSCORE, index);
                            previousUnderscore = index;
                            continue;
                        case TILDE_CHAR:
                            formatMarkers.addPaired(FormatMarker.TILDE, FormatMarker.TILDE_PAIR, index);
                            continue;
                        case PERCENTAGE_CHAR:
                            if (text.startsWith(FORMATTING_CODE_FONT_SUFFIX, index)) {
                                formatMarkers.add(FormatMarker.FONT_SUFFIX, index);
                            } else if (text.startsWith(FORMATTING_CODE_HEX_COLOR_SUFFIX, index)) {
                                formatMarkers.add(FormatMarker.COLOR_SUFFIX, index);
                            } else {
                                // The underscores in the name of a font are no markers, even if the prefix stays text because nothing closes the font
                                int fontPrefixEnd = getFontPrefixEnd(text, index, lineEnd);
                                if (fontPrefixEnd != -1) index = fontPrefixEnd - 1;
                            }
                            continue;
                        default:
                            break;
                    }
                }

                //Handle Hyperlink Image
                if (isStartOfLine && (c == OPEN_SQUARE_BRACKETS_CHAR) && markdown && text.startsWith(FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX, index)) {
                    HyperlinkImage hyperlinkImage = getHyperlinkImageFromLine(text.substring(index, lineEnd));
                    if (hyperlinkImage != null) {
                        if (this.hyperlinkImages == null) this.hyperlinkImages = new ArrayList<>();
                        this.hyperlinkImages.add(hyperlinkImage);
                        this.addEvent(index, Event.HYPERLINK_IMAGE, this.hyperlinkImages.size() - 1, 0);
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                }

                //Handle Image
                if (isStartOfLine && (c == EXCLAMATION_MARK_CHAR) && markdown && text.startsWith(FORMATTING_CODE_IMAGE_PREFIX, index)) {
                    int labelEnd = getLinkLabelEnd(text, index + FORMATTING_CODE_IMAGE_PREFIX.length());
                    int sourceEnd = (labelEnd != -1) ? getLinkTargetEnd(text, labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length()) : -1;
                    if (sourceEnd != -1) {
                        this.addEvent(index, Event.IMAGE, labelEnd, sourceEnd);
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                }

                //Handle Hyperlink
                if ((c == OPEN_SQUARE_BRACKETS_CHAR) && (linkLabelEnd == NO_LINK) && markdown) {
                    int labelEnd = getLinkLabelEnd(text, index + 1);
                    int targetEnd = (labelEnd != -1) ? getLinkTargetEnd(text, labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length()) : -1;
                    if (targetEnd != -1) {
                        linkOpenEvent = this.eventsEnd;
                        this.addEvent(index, Event.LINK_OPEN, labelEnd, targetEnd);
                        linkLabelEnd = labelEnd;
                        linkTargetEnd = targetEnd;
                        continue;
                    }
                }
                // The link ends at the bracket its label was found to end at, and at no other one.
                // The characters behind that bracket are exactly the target that was read back then, so skipping them never cuts into other text.
                if (index == linkLabelEnd) {
                    if (markdown) {
                        this.addEvent(index, Event.LINK_CLOSE, linkTargetEnd, 0);
                        index = linkTargetEnd;
                        linkLabelEnd = NO_LINK;
                        continue;
                    }
                    // The bracket is text of code or of plain text that opened inside of the label, so the label ends somewhere behind that
                    linkLabelEnd = LINK_LABEL_END_UNKNOWN;
                }

                //Handle Quote
                if (isStartOfLine && (c == GREATER_THAN_CHAR) && markdown) {
                    // The prefix opens a quote, and lines of an open quote that start with it stay part of that quote without showing it
                    if (text.startsWith(FORMATTING_CODE_QUOTE_PREFIX, index)) {
                        if (!quote) {
                            quote = true;
                            CalloutType calloutType = callouts ? getCalloutType(text, index + FORMATTING_CODE_QUOTE_PREFIX.length(), lineEnd) : null;
                            if (calloutType != null) {
                                this.addEvent(index, Event.CALLOUT, calloutType.ordinal(), 0);
                                index = lineEnd;
                                queueNewLine = true;
                                continue;
                            }
                            this.addEvent(index, Event.QUOTE_OPEN, 0, 0);
                        } else {
                            this.addEvent(index, Event.QUOTE_PREFIX, 0, 0);
                        }
                        index++;
                        continue;
                    }
                    // A line with nothing but the quote character is an empty line of the open quote
                    if (quote && ((lineEnd - index) == 1)) {
                        this.addEvent(index, Event.QUOTE_EMPTY_LINE, 0, 0);
                        continue;
                    }
                }
                // An empty line ends the quote. Empty lines of a code block or of plain text inside of the quote are text of those.
                if (isStartOfLine && quote && markdown && containsOnly(text, index, lineEnd, IS_TRIMMED_WHITESPACE)) {
                    quote = false;
                    // The rest of the line still counts as the start of a line
                    this.addEvent(index, Event.QUOTE_END, 0, 0);
                    queueNewLine = true;
                    continue;
                }

                //Handle Separation Line
                if (isStartOfLine && (c == MINUS_CHAR) && markdown && text.startsWith(FORMATTING_CODE_SEPARATION_LINE_PREFIX, index) && containsOnly(text, index, lineEnd, IS_SEPARATION_LINE_CHARACTER)) {
                    this.addEvent(index, Event.SEPARATION_LINE, 0, 0);
                    index = lineEnd;
                    queueNewLine = true;
                    continue;
                }

                //Handle Code Block Single Line
                if ((c == GRAVE_ACCENT_CHAR) && !plainText) {
                    // A grave accent with a second one right behind it opens nothing: the second one would close the block around nothing,
                    // and both would vanish. That keeps the marker of a multi-line block that never gets closed in the text as well.
                    // The block ends at a grave accent of the same line that does not start a multi-line marker.
                    // The last grave accent of every row of grave accents is one, so any later one in the line means there is such an end.
                    if (!code && !text.startsWith(FORMATTING_CODE_CODE_BLOCK_SINGLE_LINE_EMPTY, index) && lookahead.existsBetween(LookaheadMarker.CODE_BLOCK_SINGLE_LINE, index + 1, lineEnd)) {
                        this.addEvent(index, Event.CODE_OPEN, 0, 0);
                        code = true;
                        singleLineCode = true;
                        continue;
                    }
                    if (code && singleLineCode && !text.startsWith(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, index)) {
                        this.addEvent(index, Event.CODE_CLOSE, 0, 0);
                        code = false;
                        if (linkLabelEnd == LINK_LABEL_END_UNKNOWN) {
                            linkLabelEnd = this.findLinkLabelEndAgain(text, linkOpenEvent, index + 1);
                            linkTargetEnd = this.events[linkOpenEvent + 3];
                        }
                        continue;
                    }
                }

                //Handle Code Block Multi Line
                if (isStartOfLine && (c == GRAVE_ACCENT_CHAR) && !plainText && text.startsWith(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, index)) {
                    int markerEnd = index + FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX.length();
                    // The marker that opens a block can have an info string behind it, like the name of a language, which is no part of the block.
                    // A grave accent behind the marker makes the line text with code inside of it instead.
                    if (!code && (indexOfMarkerCharacter(text, GRAVE_ACCENT_CHAR, markerEnd, lineEnd) == -1) && lookahead.existsFrom(LookaheadMarker.CODE_BLOCK_MULTI_LINE_END, lineEnd)) {
                        this.addEvent(index, Event.CODE_BLOCK_OPEN, 0, 0);
                        code = true;
                        singleLineCode = false;
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                    // The marker that closes a block has nothing but whitespace behind it
                    if (code && !singleLineCode && containsOnly(text, markerEnd, lineEnd, IS_TRIMMED_WHITESPACE)) {
                        this.addEvent(index, Event.CODE_BLOCK_CLOSE, 0, 0);
                        code = false;
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                }

                //Handle Plain Text Single Line
                // Plain text opens nowhere inside of code, where its markers are text like everything else
                if ((c == SEMICOLON_CHAR) && !code && !text.startsWith(FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, index) && text.startsWith(FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX, index)) {
                    if (!plainText && lookahead.existsFrom(LookaheadMarker.PLAIN_TEXT_SINGLE_LINE, index + 2)) {
                        this.addEvent(index, Event.PLAIN_TEXT_OPEN, 0, 0);
                        plainText = true;
                        index++;
                        continue;
                    }
                    if (plainText) {
                        this.addEvent(index, Event.PLAIN_TEXT_CLOSE, 0, 0);
                        plainText = false;
                        index++;
                        if (linkLabelEnd == LINK_LABEL_END_UNKNOWN) {
                            linkLabelEnd = this.findLinkLabelEndAgain(text, linkOpenEvent, index + 1);
                            linkTargetEnd = this.events[linkOpenEvent + 3];
                        }
                        continue;
                    }
                }

                //Handle Plain Text Multi Line
                // The marker line has nothing but whitespace behind its marker
                if (isStartOfLine && (c == SEMICOLON_CHAR) && !code && text.startsWith(FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, index) && containsOnly(text, index + FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX.length(), lineEnd, IS_TRIMMED_WHITESPACE)) {
                    if (!plainText && lookahead.existsFrom(LookaheadMarker.PLAIN_TEXT_MULTI_LINE_END, lineEnd)) {
                        this.addEvent(index, Event.PLAIN_TEXT_BLOCK_OPEN, 0, 0);
                        plainText = true;
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                    if (plainText) {
                        this.addEvent(index, Event.PLAIN_TEXT_BLOCK_CLOSE, 0, 0);
                        plainText = false;
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                }

                //Handle Alignment
                if (isStartOfLine && markdown && ((c == CIRCUMFLEX_CHAR) || (c == VERTICAL_BAR_CHAR))) {
                    boolean centered = (c == CIRCUMFLEX_CHAR);
                    String marker = centered ? FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX : FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX;
                    // The marker line has nothing but whitespace behind its marker
                    if (text.startsWith(marker, index) && containsOnly(text, index + marker.length(), lineEnd, IS_TRIMMED_WHITESPACE)) {
                        // What the marker does depends on the markers behind it, so that gets settled when all of them are found.
                        // The line holds nothing the pass has to read, so jumping over it is right even if the marker turns out to be text.
                        this.addEvent(index, Event.ALIGNMENT, (centered ? MarkdownRenderer.MarkdownLineAlignment.CENTERED : MarkdownRenderer.MarkdownLineAlignment.RIGHT).ordinal(), 0);
                        if (centered) {
                            centeredAlignmentMarkers++;
                        } else {
                            rightAlignmentMarkers++;
                        }
                        index = lineEnd;
                        queueNewLine = true;
                        continue;
                    }
                }

            }

            if (c == NEWLINE_CHAR) queueNewLine = true;

        }

        if ((centeredAlignmentMarkers > 0) || (rightAlignmentMarkers > 0)) this.settleAlignmentMarkers(centeredAlignmentMarkers, rightAlignmentMarkers);

        // A link needs the end of its label. Code and plain text always close before the text ends, so this only happens
        // if something else took the marker that closes them, like a custom formatting code that uses a character of that marker.
        if (linkLabelEnd == LINK_LABEL_END_UNKNOWN) this.removeEvent(linkOpenEvent);

        if (this.eventsEnd > 0) this.nextEventIndex = this.events[0];

    }

    /**
     * Settles what the marker lines of aligned blocks do, which all are events with the alignment of their marker as value so far.
     *
     * <p>A marker opens its block in text that is not aligned if a later marker of the same alignment closes it, and the next such marker does.
     * Every other marker is text and loses its event: one without a later marker to close its block, and one inside the block of the other alignment.</p>
     *
     * <p>Counting the markers that follow, instead of searching the text for one, only counts lines the parser reads as markers.
     * That leaves out lines of code blocks and plain text, which are text.</p>
     *
     * @param centeredMarkers The number of marker lines for centered blocks.
     * @param rightMarkers The number of marker lines for right-aligned blocks.
     */
    private void settleAlignmentMarkers(int centeredMarkers, int rightMarkers) {
        int left = MarkdownRenderer.MarkdownLineAlignment.LEFT.ordinal();
        int centered = MarkdownRenderer.MarkdownLineAlignment.CENTERED.ordinal();
        int alignment = left;
        for (int event = 0; event < this.eventsEnd; event += EVENT_SIZE) {
            if (this.events[event + 1] != Event.ALIGNMENT.ordinal()) continue;
            int markerAlignment = this.events[event + 2];
            int laterMarkers = (markerAlignment == centered) ? --centeredMarkers : --rightMarkers;
            if ((alignment == left) && (laterMarkers > 0)) {
                alignment = markerAlignment;
            } else if (alignment == markerAlignment) {
                alignment = left;
                this.events[event + 2] = left;
            } else {
                this.removeEvent(event);
            }
        }
    }

    /**
     * Finds the end of the label of the open link behind code or plain text that held the bracket the label was found to end at before.<br>
     * The event that opened the link gets the new end of its label and of its target. Without a new end, there is no link, and that event gets removed.
     *
     * @param linkOpenEvent Where the event that opened the link is in the events.
     * @param fromIndex The index behind the code or plain text.
     * @return The index of the bracket that ends the label, or {@link #NO_LINK}.
     */
    private int findLinkLabelEndAgain(@NotNull String text, int linkOpenEvent, int fromIndex) {
        int labelEnd = getLinkLabelEnd(text, fromIndex);
        int targetEnd = (labelEnd != -1) ? getLinkTargetEnd(text, labelEnd + FORMATTING_CODE_HYPERLINK_INNER_PREFIX.length()) : -1;
        if (targetEnd == -1) {
            this.removeEvent(linkOpenEvent);
            return NO_LINK;
        }
        this.events[linkOpenEvent + 2] = labelEnd;
        this.events[linkOpenEvent + 3] = targetEnd;
        return labelEnd;
    }

    /**
     * Makes the parser read the character of an event as if there was no event, which only works for events it has not read.
     *
     * @param event Where the event is in the events.
     */
    private void removeEvent(int event) {
        this.events[event + 1] = Event.NONE.ordinal();
    }

    private void addEvent(int index, @NotNull Event event, int value, int secondValue) {
        if (this.eventsEnd == this.events.length) this.events = Arrays.copyOf(this.events, Math.max(16 * EVENT_SIZE, this.events.length * 2));
        this.events[this.eventsEnd++] = index;
        this.events[this.eventsEnd++] = event.ordinal();
        this.events[this.eventsEnd++] = value;
        this.events[this.eventsEnd++] = secondValue;
    }

    /**
     * Returns the event at the given index of the text, which is {@link Event#NONE} for most characters.<br>
     * The parser asks for every character it reads, in the order of the text. The values of a returned event
     * are {@link #getEventValue()} and {@link #getSecondEventValue()} until the next event gets returned.
     */
    @NotNull
    Event getEventAt(int index) {
        if (index < this.nextEventIndex) return Event.NONE;
        Event event = Event.NONE;
        if (index == this.nextEventIndex) {
            event = EVENT_TYPES[this.events[this.nextEvent + 1]];
            this.eventValue = this.events[this.nextEvent + 2];
            this.secondEventValue = this.events[this.nextEvent + 3];
        }
        // The parser reads the character of every event, so this moves on by one event. Doing it in a loop keeps an event
        // the parser jumped over, which is a mistake in the parser or the pass, from holding back all events behind it.
        while ((this.nextEvent < this.eventsEnd) && (this.events[this.nextEvent] <= index)) this.nextEvent += EVENT_SIZE;
        this.nextEventIndex = (this.nextEvent < this.eventsEnd) ? this.events[this.nextEvent] : Integer.MAX_VALUE;
        return event;
    }

    int getEventValue() {
        return this.eventValue;
    }

    int getSecondEventValue() {
        return this.secondEventValue;
    }

    @NotNull
    HyperlinkImage getHyperlinkImage(int index) {
        return Objects.requireNonNull(this.hyperlinkImages).get(index);
    }

    @NotNull
    MarkdownFormatMarkers getFormatMarkers() {
        return this.formatMarkers;
    }

    /**
     * What the parser does at a character of the text. The comments name the two values of every event that has some.
     */
    enum Event {

        /** Nothing: the character is text, or part of a format the parser decides about on its own. */
        NONE(false),
        /** A table starts, which takes all of its lines. Value: the index of the table in the list of tables. */
        TABLE(true),
        /** The line break behind a headline gets skipped, because the line behind it is empty. */
        SKIPPED_LINE_BREAK(false),
        /** A Minecraft formatting code starts, which is this character and the next one. */
        FORMATTING_CODE(false),
        /** The backslash of an escaped character, which makes the next character text. */
        ESCAPE(false),
        /** The prefix of a headline starts. Value: the number of hashtags of the prefix, which is 1 for the biggest headline and 3 for the smallest one. */
        HEADLINE(false),
        /** A list item starts with its indent, its marker and a space. Values: the index of the marker, and the index of the space behind it. */
        LIST_ITEM(false),
        /** A line that is an image with a hyperlink. Value: the index of the image for {@link #getHyperlinkImage(int)}. */
        HYPERLINK_IMAGE(true),
        /** A line that is an image. Values: the index of the bracket that ends its alternative text, and the index of the bracket that ends its source. */
        IMAGE(true),
        /** A link, or a text with a click or hover event, starts. Values: the index of the bracket that ends its label, and the index of the bracket that ends its target. */
        LINK_OPEN(false),
        /** The label of the open link ends, and its target follows. Value: the index of the bracket that ends the target. */
        LINK_CLOSE(false),
        /** The prefix of a quote line that opens a quote. */
        QUOTE_OPEN(false),
        /** The prefix of a line of the open quote. */
        QUOTE_PREFIX(false),
        /** The marker line of a callout, which opens a quote. Value: the ordinal of the callout type. */
        CALLOUT(true),
        /** The quote character of an empty line of the open quote. */
        QUOTE_EMPTY_LINE(false),
        /** The first character of the empty line that ends the open quote. */
        QUOTE_END(false),
        /** A line that is a separation line. */
        SEPARATION_LINE(true),
        /** The marker that opens code inside of a line. */
        CODE_OPEN(false),
        /** The marker that closes code inside of a line. */
        CODE_CLOSE(false),
        /** The marker line that opens a code block. */
        CODE_BLOCK_OPEN(true),
        /** The marker line that closes a code block. */
        CODE_BLOCK_CLOSE(true),
        /** The two characters that open plain text. */
        PLAIN_TEXT_OPEN(false),
        /** The two characters that close plain text. */
        PLAIN_TEXT_CLOSE(false),
        /** The marker line that opens plain text. */
        PLAIN_TEXT_BLOCK_OPEN(true),
        /** The marker line that closes plain text. */
        PLAIN_TEXT_BLOCK_CLOSE(true),
        /** The marker line that opens or closes an aligned block. Value: the ordinal of the alignment of the lines behind it. A marker line that does neither is text and no event. */
        ALIGNMENT(true);

        /** If the event stands for everything up to the end of its line, so the parser reads nothing else of that line. */
        final boolean wholeLine;

        Event(boolean wholeLine) {
            this.wholeLine = wholeLine;
        }

    }

    /**
     * The markers the pass looks ahead for to find out if something it is about to open gets closed again.
     */
    private enum LookaheadMarker {

        // A grave accent closes its block even behind a backslash, because nothing gets escaped inside the block
        CODE_BLOCK_SINGLE_LINE(String.valueOf(GRAVE_ACCENT_CHAR), false),
        PLAIN_TEXT_SINGLE_LINE(FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX, false),
        CODE_BLOCK_MULTI_LINE_END(FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX, true),
        // A line that starts with the marker of multi-line plain text always ends plain text: at that marker, or at the two semicolons that close plain text inside of a line
        PLAIN_TEXT_MULTI_LINE_END(NEWLINE + FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX, false);

        static final int COUNT = values().length;

        @NotNull
        final String marker;
        /** If the marker only counts where it is a line of its own, which {@link MarkerLookahead#isMarkerLine(LookaheadMarker, int)} tells. */
        final boolean line;

        LookaheadMarker(@NotNull String marker, boolean line) {
            this.marker = marker;
            this.line = line;
        }

    }

    /**
     * Finds the next occurrence of {@link LookaheadMarker}s for the pass, which only moves forward through the text.
     *
     * <p>A found occurrence answers every following search for its marker until the pass has moved past it, and a failed
     * search answers every following search for the rest of the text. All searches for one marker together read the text
     * only once that way, no matter how many markers without a closing one the text holds.</p>
     *
     * <p>This only works as long as the searches for a marker never start at a smaller index than an earlier one did.</p>
     *
     * <p>The parser jumps over tables, so a marker that touches a table closes nothing. A table inside a code block or plain text is text
     * of that block instead: while the pass reads the lines of such a table, the markers in those lines count like all other ones.</p>
     */
    private static final class MarkerLookahead {

        static final int NOT_SEARCHED = -2;
        static final int NOT_FOUND = -1;

        @NotNull
        final String text;
        /** The tables of the text, in the order of their lines. */
        @NotNull
        final List<Table> tables;
        /** The next occurrence of every marker in the whole text, tables included. */
        final int[] nextOccurrences = new int[LookaheadMarker.COUNT];
        /** The next occurrence of every marker that touches no table. */
        final int[] nextOccurrencesOutsideTables = new int[LookaheadMarker.COUNT];
        /** For every marker, the index of the first table that does not end in front of {@link #nextOccurrencesOutsideTables its next occurrence outside of tables}. */
        final int[] nextTables = new int[LookaheadMarker.COUNT];
        /** The index behind the table whose lines the pass reads as text, which is smaller than the index of the pass when it reads no table. */
        int textTableEnd = -1;

        MarkerLookahead(@NotNull String text, @NotNull List<Table> tables) {
            this.text = text;
            this.tables = tables;
            Arrays.fill(this.nextOccurrences, NOT_SEARCHED);
            Arrays.fill(this.nextOccurrencesOutsideTables, NOT_SEARCHED);
        }

        /**
         * Makes the markers in the lines of the given table count for as long as the searches start in front of the end of that table.
         */
        void readTableAsText(@NotNull Table table) {
            this.textTableEnd = table.end();
        }

        /**
         * Returns the index of the first occurrence of the marker at or after the given index, or -1 if there is none.
         */
        int indexOf(@NotNull LookaheadMarker marker, int fromIndex) {
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
        int indexOf(@NotNull LookaheadMarker marker, int fromIndex, int[] nextOccurrences, boolean outsideTables) {
            // The remembered occurrence is the next one behind every index a search started at before
            int nextOccurrence = nextOccurrences[marker.ordinal()];
            if ((nextOccurrence != NOT_FOUND) && (nextOccurrence < fromIndex)) {
                nextOccurrence = this.text.indexOf(marker.marker, fromIndex);
                while (nextOccurrence != NOT_FOUND) {
                    Table table = outsideTables ? this.getTableTouchedBy(marker, nextOccurrence) : null;
                    if (table != null) {
                        nextOccurrence = this.text.indexOf(marker.marker, table.end());
                    } else if (isFormattingCodeCharacter(this.text, nextOccurrence) || (marker.line && !this.isMarkerLine(marker, nextOccurrence))) {
                        // The character of a formatting code is no marker, whatever it is
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
         * Returns if the marker that starts at the given index is a line of its own, the way the pass reads the line when it gets there:
         * nothing but whitespace follows the marker, and nothing but formatting codes stands in front of it, which leave the start of a line to what follows them.
         *
         * <p>Both checks stop at the first character that does not fit. Checking every occurrence of a marker this way reads
         * the whitespace behind each one and the formatting codes in front of each one once, which keeps all searches together linear.</p>
         */
        boolean isMarkerLine(@NotNull LookaheadMarker marker, int occurrence) {
            int length = this.text.length();
            for (int index = occurrence + marker.marker.length(); (index < length) && (this.text.charAt(index) != NEWLINE_CHAR); index++) {
                if (!IS_TRIMMED_WHITESPACE.test(this.text.charAt(index))) return false;
            }
            int lineStart = occurrence;
            while ((lineStart >= 2) && (this.text.charAt(lineStart - 1) != NEWLINE_CHAR) && isFormattingCode(this.text, lineStart - 2)) lineStart -= 2;
            return (lineStart == 0) || (this.text.charAt(lineStart - 1) == NEWLINE_CHAR);
        }

        /**
         * Returns the table that holds at least one character of the marker that starts at the given index, or NULL if there is none.<br>
         * Like the searches for a marker, this never gets asked for a smaller index than before.
         */
        @Nullable
        Table getTableTouchedBy(@NotNull LookaheadMarker marker, int occurrence) {
            int nextTable = this.nextTables[marker.ordinal()];
            while ((nextTable < this.tables.size()) && (this.tables.get(nextTable).end() <= occurrence)) nextTable++;
            this.nextTables[marker.ordinal()] = nextTable;
            if (nextTable == this.tables.size()) return null;
            Table table = this.tables.get(nextTable);
            // Markers that start with a line break can start in front of a table and reach into its first line
            return ((occurrence + marker.marker.length()) > table.start()) ? table : null;
        }

        /**
         * Returns if the marker starts at or after the given index.
         */
        boolean existsFrom(@NotNull LookaheadMarker marker, int fromIndex) {
            return this.indexOf(marker, fromIndex) != NOT_FOUND;
        }

        /**
         * Returns if the marker starts at or after the given index and before the end index.
         */
        boolean existsBetween(@NotNull LookaheadMarker marker, int fromIndex, int endIndex) {
            int nextOccurrence = this.indexOf(marker, fromIndex);
            return (nextOccurrence != NOT_FOUND) && (nextOccurrence < endIndex);
        }

    }

}
