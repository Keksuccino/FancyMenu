package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chinese and Japanese put no spaces between words, so lines of such text end inside of fragments, at the places {@link MarkdownLineBreaks} allows.
 * The layout splits a fragment a line ends inside of into one piece for every line it is in, and text that fits into its line stays the fragment it was.
 */
class MarkdownSpacelessTextWrapTest {

    /** Six pixels per character and two pixels of padding on both sides make ten characters fit into a line. */
    private static final float TEN_CHARACTERS = 64;

    @Test
    void textWithoutSpacesWrapsBetweenItsCharacters() {
        MarkdownTestRenderer renderer = render("一二三四五六七八九十一二三四五六七八九十一二三四五", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九十", "一二三四五六七八九十", "一二三四五"), lineTexts(renderer));
        for (MarkdownTextLine line : renderer.lines) assertTrue(line.getLineWidth() <= 60);
        assertEquals(TEN_CHARACTERS, renderer.getRealWidth());
    }

    @Test
    void textThatFitsStaysTheFragmentItWasAndGetsMeasuredOnlyOnce() {
        MarkdownTestRenderer renderer = render("日本語のテキスト、**太字**と[リンク](https://example.invalid)です。", 600);
        List<MarkdownTextFragment> fragments = new ArrayList<>(renderer.fragments);
        int measured = renderer.testFont().measuredFragmentTexts.size();

        assertEquals(1, renderer.lines.size());
        assertEquals(fragments, renderer.lines.get(0).fragments);

        renderer.setOptimalWidth(500);
        renderer.tick();

        assertEquals(1, renderer.lines.size());
        for (int i = 0; i < fragments.size(); i++) assertSame(fragments.get(i), renderer.lines.get(0).fragments.get(i));
        // The layout only measures text of a fragment a line ends inside of
        assertEquals(measured, renderer.testFont().measuredFragmentTexts.size());
    }

    @ParameterizedTest
    @ValueSource(chars = {'、', '。', '，', '．', '！', '？', '」', '』', '）', '】', 'っ', 'ゃ', 'ょ', 'ッ', 'ー', '々'})
    void characterThatMustNotStartALineTakesTheCharacterInFrontOfItToTheNextLine(char character) {
        // The character is the eleventh one, which is the first one that does not fit
        MarkdownTestRenderer renderer = render("あいうえおかきくけこ" + character + "さしす", TEN_CHARACTERS);

        assertEquals(List.of("あいうえおかきくけ", "こ" + character + "さしす"), lineTexts(renderer));
    }

    @ParameterizedTest
    @ValueSource(chars = {'「', '『', '（', '【'})
    void characterThatMustNotEndALineGoesToTheNextLine(char character) {
        // The character is the tenth one, which is the last one that fits
        MarkdownTestRenderer renderer = render("あいうえおかきくけ" + character + "こさしす", TEN_CHARACTERS);

        assertEquals(List.of("あいうえおかきくけ", character + "こさしす"), lineTexts(renderer));
    }

    @Test
    void koreanTextOnlyWrapsAtItsSpaces() {
        MarkdownTestRenderer renderer = render("한국어문장입니다 한국어문장입니다 한국어 문장", TEN_CHARACTERS);

        assertEquals(List.of("한국어문장입니다 ", "한국어문장입니다 ", "한국어 문장"), lineTexts(renderer));
        assertEquals(renderer.fragments, allLineFragments(renderer));

        // A word that is wider than a line stays whole
        renderer = render("한국어문장입니다한국어문장입니다", TEN_CHARACTERS);
        assertEquals(List.of("한국어문장입니다한국어문장입니다"), lineTexts(renderer));
    }

    @Test
    void wordsOfOtherScriptsInsideOfTheTextStayWhole() {
        MarkdownTestRenderer renderer = render("日本語とMinecraftのテキストとFancyMenuです", TEN_CHARACTERS);

        assertEquals(List.of("日本語と", "Minecraftの", "テキストと", "FancyMenuで", "す"), lineTexts(renderer));
    }

    @Test
    void lineFillsUpWithTheTextBehindAWord() {
        MarkdownTestRenderer renderer = render("word 一二三四五六七八九十", TEN_CHARACTERS);

        // The text does not fit behind the word as a whole, but its first characters do
        assertEquals(List.of("word 一二三四五", "六七八九十"), lineTexts(renderer));
    }

    @Test
    void lineWrapsInFrontOfTextWhoseFirstCharacterDoesNotFit() {
        MarkdownTestRenderer renderer = render("ninechars 一二三四五", TEN_CHARACTERS);

        assertEquals(List.of("ninechars ", "一二三四五"), lineTexts(renderer));
        assertSame(renderer.fragments.get(1), renderer.lines.get(1).fragments.get(0));
    }

    @Test
    void spaceBehindTheTextStaysWithItsLastPiece() {
        MarkdownTestRenderer renderer = render("一二三四五六七八九十一二三 next word", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九十", "一二三 next ", "word"), lineTexts(renderer));
        MarkdownTextFragment first = renderer.lines.get(0).fragments.get(0);
        MarkdownTextFragment last = renderer.lines.get(1).fragments.get(0);
        assertFalse(first.endOfWord);
        assertTrue(first.autoLineBreakAfter);
        assertTrue(last.endOfWord);
        assertEquals("一二三 ", last.text);
    }

    @Test
    void lineBreakOfTheTextStaysBehindTheLastPiece() {
        MarkdownTestRenderer renderer = render("一二三四五六七八九十一二三\n四五六", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九十", "一二三", "四五六"), lineTexts(renderer));
        assertFalse(renderer.lines.get(0).fragments.get(0).naturalLineBreakAfter);
        assertTrue(renderer.lines.get(1).fragments.get(0).naturalLineBreakAfter);
    }

    @Test
    void lineCanEndWhereTheFormatChanges() {
        MarkdownTestRenderer renderer = render("一二三四五六**七八九十**一二三四五六", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九十", "一二三四五六"), lineTexts(renderer));
        assertEquals(renderer.fragments, allLineFragments(renderer));
        assertTrue(renderer.lines.get(0).fragments.get(1).bold);
        assertFalse(renderer.lines.get(1).fragments.get(0).bold);
    }

    @Test
    void piecesAreFormattedLikeTheirFragment() {
        MarkdownTestRenderer renderer = render("一二三**四五六七八九十一二三四五六七八九十一二三**四五六", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九十", "一二三四五六七八九十", "一二三四五六"), lineTexts(renderer));
        assertEquals(List.of("一二三", "四五六七八九十"), MarkdownParserTestSupport.texts(renderer.lines.get(0).fragments));
        assertEquals(List.of("一二三", "四五六"), MarkdownParserTestSupport.texts(renderer.lines.get(2).fragments));
        assertFalse(renderer.lines.get(0).fragments.get(0).bold);
        assertTrue(renderer.lines.get(0).fragments.get(1).bold);
        assertTrue(renderer.lines.get(1).fragments.get(0).bold);
        assertTrue(renderer.lines.get(2).fragments.get(0).bold);
        assertFalse(renderer.lines.get(2).fragments.get(1).bold);
        // The fragment is its own first piece
        assertSame(renderer.fragments.get(1), renderer.lines.get(0).fragments.get(1));
    }

    /**
     * This test knows every field of a fragment: the ones all pieces of a fragment share, and the ones every piece has for itself.
     * A new field has to become one of the two here, and what the pieces share has to be given to every piece that gets added.
     */
    @Test
    void pieceSharesEverythingThatFormatsItsFragment() throws ReflectiveOperationException {
        Set<String> shared = Set.of("parent", "unscaledTextHeight", "textColor", "bold", "italic", "strikethrough", "minecraftFormatting", "bulletListLevel", "listItemMarker",
                "listItemNumber", "listHasPlusItems", "alignment", "hyperlink", "clickEvent", "hoverEvent", "headlineType", "quoteContext", "codeBlockContext", "plainText", "font");
        Set<String> own = Set.of("parentLine", "text", "x", "y", "unscaledTextWidth", "unscaledTrailingSpaceWidth", "startOfRenderLine", "naturalLineBreakAfter", "autoLineBreakAfter",
                "endOfWord", "strikethroughEnd", "hovered", "breaksInside", "pieces", "renderComponent", "fixedWidthText", "renderComponentState", "renderComponentStyleVersion", "renderComponentCustomColor",
                // What marks a fragment as the first one of something, and what only fragments have that no line ends inside of
                "bulletListItemStart", "calloutMarker", "imageSupplier", "imageAltText", "imageCaption", "separationLine", "tableContext", "table", "tableHeader");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("", 600);
        MarkdownTextFragment plain = new MarkdownTextFragment(renderer, "一二三四");
        MarkdownTextFragment fragment = new MarkdownTextFragment(renderer, "一二三四");
        fragment.unscaledTextHeight = 20;
        fragment.textColor = DrawableColor.of(1, 2, 3);
        fragment.bold = true;
        fragment.italic = true;
        fragment.strikethrough = true;
        fragment.minecraftFormatting = MinecraftFormattingState.EMPTY.apply(ChatFormatting.UNDERLINE);
        fragment.bulletListLevel = 2;
        fragment.listItemMarker = MarkdownTextFragment.ListItemMarker.NUMBER;
        fragment.listItemNumber = "1.";
        fragment.listHasPlusItems = true;
        fragment.alignment = MarkdownRenderer.MarkdownLineAlignment.CENTERED;
        fragment.hyperlink = new MarkdownTextFragment.Hyperlink();
        fragment.clickEvent = new MarkdownTextFragment.TextClickEvent("click");
        fragment.hoverEvent = new MarkdownTextFragment.TextHoverEvent("hover");
        fragment.headlineType = MarkdownTextFragment.HeadlineType.BIG;
        fragment.quoteContext = new MarkdownTextFragment.QuoteContext();
        fragment.codeBlockContext = new MarkdownTextFragment.CodeBlockContext();
        fragment.plainText = true;
        fragment.font = ResourceLocation.withDefaultNamespace("uniform");

        fragment.setPieceText(fragment, 0, 2, 12.0F);
        MarkdownTextFragment piece = fragment.addPiece();
        fragment.setPieceText(piece, 2, 4, 12.0F);

        assertEquals("一二", fragment.text);
        assertEquals("三四", piece.text);
        for (Field field : MarkdownTextFragment.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            String name = field.getName();
            assertTrue(shared.contains(name) != own.contains(name), "This test has to know the field: " + name);
            if (!shared.contains(name)) continue;
            assertEquals(field.get(fragment), field.get(piece), name);
            // The fragment has another value than a new fragment, so the piece got its value from the fragment
            if (!name.equals("parent")) assertNotEquals(field.get(plain), field.get(fragment), name);
        }
    }

    @Test
    void characterThatMustNotStartALineStaysBehindTheFormattedTextInFrontOfIt() {
        // The full stop is the eleventh character. It stays with the last bold character, which goes to the next line with it.
        MarkdownTestRenderer renderer = render("一二三四五六七八**九十**。一二", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九", "十。一二"), lineTexts(renderer));
        assertEquals(List.of("十", "。一二"), MarkdownParserTestSupport.texts(renderer.lines.get(1).fragments));
        assertTrue(renderer.lines.get(0).fragments.get(1).bold);
        assertTrue(renderer.lines.get(1).fragments.get(0).bold);
        assertFalse(renderer.lines.get(1).fragments.get(1).bold);
    }

    @Test
    void characterThatMustNotEndALineStaysInFrontOfTheFormattedTextBehindIt() {
        // The bracket is the tenth character and the bold text starts behind it, so the line ends in front of the bracket
        MarkdownTestRenderer renderer = render("一二三四五六七八九「**十一**」二三", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九", "「十一」二三"), lineTexts(renderer));
    }

    @Test
    void piecesOfALinkAreHoveredAndClickedAsOneLink() {
        MarkdownTestRenderer renderer = render("[一二三四五六七八九十一二三四五六七八九十一二三四五](https://example.invalid)", TEN_CHARACTERS);
        List<String> clicked = new ArrayList<>();
        renderer.setHyperlinkFilter(link -> {
            clicked.add(link);
            return false;
        });
        List<MarkdownTextFragment> pieces = linkLikePieces(renderer);
        MarkdownTextFragment first = pieces.get(0);
        MarkdownTextFragment second = pieces.get(1);
        MarkdownTextFragment last = pieces.get(2);

        assertEquals(3, renderer.lines.size());
        assertEquals(3, pieces.size());
        assertNotNull(first.hyperlink);
        assertSame(first.hyperlink, second.hyperlink);
        assertSame(first.hyperlink, last.hyperlink);
        assertEquals(List.of(first), renderer.children());

        // Rendering sets the hover state of a fragment
        last.setHovered(true);
        assertTrue(first.hyperlink.isHovered());
        assertTrue(first.isLinkHovered());
        assertSame(last, renderer.getChildAt(last.getTextX() + 1, last.getTextY() + 1).orElse(null));
        assertTrue(renderer.mouseClicked(last.getTextX() + 1, last.getTextY() + 1, 0));
        assertEquals(List.of("https://example.invalid"), clicked);

        renderer.resetHovered();
        assertFalse(last.hovered);
        assertFalse(first.hyperlink.isHovered());
    }

    @Test
    void pieceThatGoesAwayWithANewLayoutDoesNotStayHovered() {
        MarkdownTestRenderer renderer = render("[一二三四五六七八九十一二三四五六七八九十一二三四五](hover:event)", TEN_CHARACTERS);
        List<MarkdownTextFragment> pieces = linkLikePieces(renderer);
        MarkdownTextFragment first = pieces.get(0);
        pieces.get(2).setHovered(true);
        assertTrue(first.hoverEvent.isHovered());

        renderer.setOptimalWidth(600);
        renderer.tick();

        assertFalse(first.hoverEvent.isHovered());
        assertEquals(List.of(first), linkLikePieces(renderer));
    }

    @Test
    void newLayoutStartsWithTheWholeFragmentsAgain() {
        String text = "一二三四五六七八九十一二三四五六七八九十一二三四五 ";
        MarkdownTestRenderer renderer = render(text + "word", TEN_CHARACTERS);
        MarkdownTextFragment fragment = renderer.fragments.get(0);
        List<String> narrowLines = lineTexts(renderer);
        assertEquals(List.of("一二三四五六七八九十", "一二三四五六七八九十", "一二三四五 word"), narrowLines);
        assertEquals("一二三四五六七八九十", fragment.text);

        renderer.setOptimalWidth(600);
        renderer.tick();

        assertEquals(List.of(text + "word"), lineTexts(renderer));
        assertEquals(renderer.fragments, allLineFragments(renderer));
        assertEquals(text, fragment.text);
        assertEquals(text.length() * 6, fragment.unscaledTextWidth);
        assertTrue(fragment.endOfWord);

        renderer.setOptimalWidth(34);
        renderer.tick();
        assertEquals(List.of("一二三四五", "六七八九十", "一二三四五", "六七八九十", "一二三四五 ", "word"), lineTexts(renderer));

        renderer.setOptimalWidth(TEN_CHARACTERS);
        renderer.tick();
        assertEquals(narrowLines, lineTexts(renderer));
        assertSame(fragment, renderer.lines.get(0).fragments.get(0));
        assertNotSame(fragment, renderer.lines.get(1).fragments.get(0));
    }

    @Test
    void piecesOfAQuoteAreLinesOfTheQuoteAndItEndsWithTheLastOne() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> 一二三四五六七八九十一二三四五六七八九十\n\nafter", 100);
        renderer.getStyle().quotePaddingTop.set(3.0F);
        renderer.getStyle().quotePaddingBottom.set(5.0F);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragments.get(0);
        MarkdownTextLine lastQuoteLine = null;
        int quoteLines = 0;
        for (MarkdownTextLine line : renderer.lines) {
            if (line.fragments.get(0).quoteContext == null) continue;
            assertSame(first.quoteContext, line.fragments.get(0).quoteContext);
            assertEquals(MarkdownBlockType.QUOTE, line.blockType);
            assertTrue(line.getLineWidth() <= (100 - 4));
            lastQuoteLine = line;
            quoteLines++;
        }

        assertTrue(quoteLines > 1);
        assertSame(first, first.quoteContext.getQuoteStart());
        assertSame(lastQuoteLine.fragments.get(lastQuoteLine.fragments.size() - 1), first.quoteContext.getQuoteEnd());
        // The padding of the panel of a quote is above its first and below its last line
        for (MarkdownTextLine line : renderer.lines) {
            if (line.fragments.get(0).quoteContext == null) continue;
            assertEquals((line == renderer.lines.get(0)) ? 3.0F : 0.0F, line.paddingTop);
            assertEquals((line == lastQuoteLine) ? 5.0F : 0.0F, line.paddingBottom);
        }
        // Every line of a quote keeps the room for its bar free, in front of its first fragment
        for (MarkdownTextLine line : renderer.lines) {
            MarkdownTextFragment fragment = line.fragments.get(0);
            if (fragment.quoteContext != null) assertEquals(first.getTextX() - first.x, fragment.getTextX() - fragment.x);
        }
    }

    @Test
    void piecesOfAListItemAreIndentedLikeItsFirstLineAndOnlyThatOneHasTheMarker() {
        MarkdownTestRenderer renderer = render("- 一二三四五六七八九十一二三四五六七八九十", 100);
        MarkdownTextFragment first = renderer.fragments.get(0);

        assertTrue(renderer.lines.size() > 1);
        for (MarkdownTextLine line : renderer.lines) {
            MarkdownTextFragment fragment = line.fragments.get(0);
            assertEquals(1, fragment.bulletListLevel);
            assertEquals(first.getTextX(), fragment.getTextX());
            assertEquals(line == renderer.lines.get(0), line.bulletListItemStartLine);
            assertTrue(line.getLineWidth() <= (100 - 4));
        }
    }

    @Test
    void piecesOfCodeKeepThePaddingOfTheCodeAtItsStartAndItsEnd() {
        MarkdownTestRenderer renderer = render("`一二三四五六七八九十一二三四五六七八九十`", TEN_CHARACTERS);
        renderer.getStyle().inlineCodePaddingLeft.set(3.0F);
        renderer.getStyle().inlineCodePaddingRight.set(5.0F);
        renderer.tick();
        List<MarkdownTextFragment> pieces = allLineFragments(renderer);
        // The parser builds an empty fragment in front of code that starts a line
        pieces.removeIf(fragment -> fragment.text.isEmpty());

        // The paddings take the room of a character in the first line, and the last character has a line of its own
        assertEquals(List.of("一二三四五六七八九", "十一二三四五六七八九", "十"), MarkdownParserTestSupport.texts(pieces));
        for (MarkdownTextFragment piece : pieces) {
            assertNotNull(piece.codeBlockContext);
            float paddingLeft = (piece == pieces.get(0)) ? 3.0F : 0.0F;
            float paddingRight = (piece == pieces.get(pieces.size() - 1)) ? 5.0F : 0.0F;
            assertEquals(paddingLeft, piece.getTextX() - piece.x, piece.text);
            assertEquals(paddingLeft + (piece.text.length() * 6) + paddingRight, piece.getRenderWidth(), piece.text);
            assertTrue(piece.parentLine.getLineWidth() <= 60);
            assertSame(piece, piece.parentLine.singleLineCodeBlockStartEndPairs.get(piece.codeBlockContext).end);
        }
        assertSame(pieces.get(pieces.size() - 1), pieces.get(0).codeBlockContext.getBlockEnd());
    }

    @Test
    void lastPieceOfStruckThroughTextEndsIt() {
        MarkdownTestRenderer renderer = render("~~一二三四五六七八九十一二三四五六七八九十一二三~~ 四五", TEN_CHARACTERS);
        List<MarkdownTextFragment> pieces = new ArrayList<>();
        for (MarkdownTextFragment fragment : allLineFragments(renderer)) {
            if (fragment.strikethrough) pieces.add(fragment);
        }

        assertEquals(3, pieces.size());
        assertFalse(pieces.get(0).strikethroughEnd);
        assertFalse(pieces.get(1).strikethroughEnd);
        assertTrue(pieces.get(2).strikethroughEnd);
        // The space behind struck through text is no part of it
        assertEquals("一二三 ", pieces.get(2).text);
        assertEquals(3 * 6, MarkdownStrikethroughPainter.getLineWidth(pieces.get(2)));
        assertEquals(10 * 6, MarkdownStrikethroughPainter.getLineWidth(pieces.get(0)));
    }

    @Test
    void headlineWrapsBetweenItsCharacters() {
        MarkdownTestRenderer renderer = render("# 一二三四五六七八九十一二三四五六七八九十", 100);

        assertTrue(renderer.lines.size() > 1);
        for (MarkdownTextLine line : renderer.lines) {
            assertEquals(MarkdownTextFragment.HeadlineType.BIGGEST, line.fragments.get(0).headlineType);
            assertTrue(line.getLineWidth() <= (100 - 4));
        }
    }

    @Test
    void linesOfCodeBlocksAndTextWithoutLineBreaksStayWhole() {
        MarkdownTestRenderer renderer = render("```\n一二三四五六七八九十一二三四五六七八九十\n```", TEN_CHARACTERS);
        assertEquals(List.of("一二三四五六七八九十一二三四五六七八九十"), lineTexts(renderer));

        renderer = new MarkdownTestRenderer("一二三四五六七八九十一二三四五六七八九十", TEN_CHARACTERS);
        renderer.setAutoLineBreakingEnabled(false);
        renderer.tick();
        assertEquals(List.of("一二三四五六七八九十一二三四五六七八九十"), lineTexts(renderer));
    }

    @Test
    void lineThatIsTooNarrowForACharacterTakesOneCharacter() {
        MarkdownTestRenderer renderer = render("一二三「四」五", 5);

        assertEquals(List.of("一", "二", "三", "「四」", "五"), lineTexts(renderer));
    }

    @Test
    void lineBreaksBehindAnIdeographicSpace() {
        String space = String.valueOf((char) 0x3000);
        MarkdownTestRenderer renderer = render("一二三四五六七八九" + space + "十一二", TEN_CHARACTERS);

        assertEquals(List.of("一二三四五六七八九" + space, "十一二"), lineTexts(renderer));
    }

    @Test
    void lineEndsAtTheLastCharacterThatFitsWhenCharactersHaveDifferentWidths() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("abcdef一二三四五六七八九十abcdefghij一二三四五六abc一二三四五六七八九十", 100);
        renderer.testFont().spacelessCharWidth = 12;
        renderer.tick();

        // 96 pixels fit into a line: a Latin letter takes 6 of them and an ideograph 12
        assertEquals(List.of("abcdef一二三四五", "六七八九十", "abcdefghij一二三", "四五六abc一二三", "四五六七八九十"), lineTexts(renderer));
    }

    @Test
    void lineEndsWhereTheTextFitsInTheCaseItGetsDrawnIn() {
        // Every one of the four letters becomes two when it gets drawn in upper case
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("ßßßß一二三四五六七八", TEN_CHARACTERS);
        renderer.setTextCase(MarkdownRenderer.TextCase.ALL_UPPER);
        renderer.tick();

        assertEquals(List.of("ßßßß一二", "三四五六七八"), lineTexts(renderer));
        assertEquals(60, renderer.lines.get(0).getLineWidth());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 12, 13})
    void randomTextsFillTheirLinesAndOnlyBreakWhereALineMayBreak(int spacelessCharWidth) {
        Random random = new Random(20261113L + spacelessCharWidth);
        String[] parts = {"一", "二", "三", "日本語", "漢字", "かな", "カタカナ", "っ", "ゃ", "ー", "、", "。", "「", "」", "（", "）", "！", "word", "Minecraft", "a", "12", "한국어", " ", " ", ",", "."};
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("", 100);
        renderer.testFont().spacelessCharWidth = spacelessCharWidth;
        for (int round = 0; round < 3000; round++) {
            StringBuilder text = new StringBuilder("文");
            int partCount = 1 + random.nextInt(40);
            for (int i = 0; i < partCount; i++) {
                String part = parts[random.nextInt(parts.length)];
                // Rows of spaces and spaces at the end of a text are a matter of their own
                if (!part.equals(" ") || (text.charAt(text.length() - 1) != ' ')) text.append(part);
            }
            String document = text.toString().trim();
            float width = 20 + random.nextInt(200);
            renderer.setText(document);
            renderer.setOptimalWidth(width);
            renderer.tick();
            assertLinesAreFilled(renderer, document, width);

            // Laying the text out again for another width and for this one gives the same lines
            List<String> lines = lineTexts(renderer);
            renderer.setOptimalWidth(20 + random.nextInt(200));
            renderer.tick();
            renderer.setOptimalWidth(width);
            renderer.tick();
            assertEquals(lines, lineTexts(renderer), document);
        }
    }

    @Test
    void randomFormattedTextsKeepTheirTextAndOnlyBreakWhereALineMayBreak() {
        Random random = new Random(977L);
        String[] parts = {"一二三", "日本語", "かな", "カタカナ", "っ", "ー", "、", "。", "「", "」", "word", "Minecraft", "12"};
        String[][] formats = {{"", ""}, {"", ""}, {"**", "**"}, {"*", "*"}, {"~~", "~~"}, {"[", "](https://example.invalid)"}, {"`", "`"}, {"%#ff0000%", "%#%"}};
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("", 100);
        for (int round = 0; round < 3000; round++) {
            StringBuilder document = new StringBuilder("文");
            StringBuilder text = new StringBuilder("文");
            int segments = 1 + random.nextInt(8);
            for (int segment = 0; segment < segments; segment++) {
                StringBuilder segmentText = new StringBuilder();
                int partCount = 1 + random.nextInt(6);
                for (int i = 0; i < partCount; i++) segmentText.append(parts[random.nextInt(parts.length)]);
                String[] format = formats[random.nextInt(formats.length)];
                // A character without a format between two formatted texts keeps their markers apart
                String separator = (random.nextInt(4) == 0) ? "間 " : "間";
                document.append(format[0]).append(segmentText).append(format[1]).append(separator);
                text.append(segmentText).append(separator);
            }
            float width = 30 + random.nextInt(150);
            renderer.setText(document.toString().trim());
            renderer.setOptimalWidth(width);
            renderer.tick();

            String expected = text.toString().trim();
            List<String> lines = lineTexts(renderer);
            assertEquals(expected, String.join("", lines), document.toString());
            int lineStart = 0;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i > 0) assertTrue(expected.charAt(lineStart - 1) == ' ' || MarkdownLineBreaks.isAllowed(expected, lineStart), () -> document + " breaks in front of <" + line + ">");
                boolean canBreakInside = (line.trim().indexOf(' ') != -1) || MarkdownLineBreaks.hasBreak(line.trim());
                assertTrue(!canBreakInside || (renderer.lines.get(i).getLineWidth() <= (width - 4)), () -> document + " is too wide in <" + line + ">");
                lineStart += line.length();
            }
        }
    }

    /**
     * Checks the lines of a text without any formatting: they hold all of the text, they only break behind a space or where a line may break,
     * they fit into the renderer unless nothing in them can go to another line, and none of them ends in front of text that would still have fit.
     */
    private static void assertLinesAreFilled(MarkdownTestRenderer renderer, String text, float width) {
        List<String> lines = lineTexts(renderer);
        assertEquals(text, String.join("", lines), text);
        float room = width - (renderer.scaling.padding() * 2);
        int lineStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String shown = line.endsWith(" ") ? line.substring(0, line.length() - 1) : line;
            float lineWidth = renderer.testFont().width(shown);
            assertEquals(lineWidth, renderer.lines.get(i).getLineWidth(), () -> text + " measured <" + line + "> wrong");
            if (i > 0) {
                boolean behindSpace = (text.charAt(lineStart - 1) == ' ');
                assertTrue(behindSpace || MarkdownLineBreaks.isAllowed(text, lineStart), () -> text + " breaks in front of <" + line + "> at width " + width);
                // The first part of this line that no line can break in did not fit into the line in front of it.
                // A whole word no line can break in needs room for the space behind it as well.
                int firstPartEnd = firstPartEnd(text, lineStart);
                String firstPart = text.substring(lineStart, firstPartEnd);
                if (!behindSpace) firstPart = firstPart.trim();
                float widthWithFirstPart = renderer.testFont().width(lines.get(i - 1) + firstPart);
                assertTrue(widthWithFirstPart > room, () -> text + " breaks too early in front of <" + line + "> at width " + width);
            }
            boolean canBreakInside = (shown.indexOf(' ') != -1) || MarkdownLineBreaks.hasBreak(shown);
            assertTrue(!canBreakInside || (lineWidth <= room), () -> text + " is too wide in <" + line + "> at width " + width);
            lineStart += line.length();
        }
    }

    /**
     * Returns the index behind the part of the text that starts at the given index and that no line can break inside of.
     */
    private static int firstPartEnd(String text, int start) {
        for (int index = start + 1; index < text.length(); index++) {
            if ((text.charAt(index - 1) == ' ') || MarkdownLineBreaks.isAllowed(text, index)) return index;
        }
        return text.length();
    }

    private static MarkdownTestRenderer render(String text, float width) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, width);
        renderer.tick();
        return renderer;
    }

    private static List<String> lineTexts(MarkdownTestRenderer renderer) {
        List<String> lines = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) lines.add(MarkdownParserTestSupport.joinText(line.fragments));
        return lines;
    }

    private static List<MarkdownTextFragment> linkLikePieces(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> pieces = allLineFragments(renderer);
        pieces.removeIf(fragment -> !fragment.isLinkLike());
        return pieces;
    }

    private static List<MarkdownTextFragment> allLineFragments(MarkdownTestRenderer renderer) {
        List<MarkdownTextFragment> fragments = new ArrayList<>();
        for (MarkdownTextLine line : renderer.lines) fragments.addAll(line.fragments);
        return fragments;
    }

}
