package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CodeBlockContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownParser {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final CharacterFilter RESOURCE_NAME_FILTER = CharacterFilter.buildResourceNameFilter();

    //TODO Mit BufferedReader arbeiten und Zeile für Zeile einlesen (testen ob bessere Qualität)
    //TODO Mit BufferedReader arbeiten und Zeile für Zeile einlesen (testen ob bessere Qualität)
    //TODO Mit BufferedReader arbeiten und Zeile für Zeile einlesen (testen ob bessere Qualität)

    @NotNull
    public static List<MarkdownTextFragment> parse(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, boolean parseMarkdown) {

        Objects.requireNonNull(renderer);
        Objects.requireNonNull(markdownText);

        List<MarkdownTextFragment> fragments = new ArrayList<>();

        FragmentBuilder builder = new FragmentBuilder(renderer);
        boolean queueNewLine = true;
        boolean italicUnderscore = false;
        int charsToSkip = 0;
        boolean skipLine = false;
        MarkdownTextFragment lastBuiltFragment = null;

        String currentLine = "";

        int index = -1;
        for (char c : markdownText.toCharArray()) {

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            index++;

            //Part of full Markdown text source, starting at the current index
            String subText = StringUtils.substring(markdownText, index);
            //Part of current line, starting at current index
            String subLine = getLine(subText);

            //Update Current Line
            if (isStartOfLine) currentLine = getLine(subText);

            //Skip Chars
            if (charsToSkip > 0) {
                charsToSkip--;
                continue;
            }

            //Skip Line
            if ((c != '\n') && skipLine) {
                continue;
            }
            if ((c == '\n') && skipLine) {
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                skipLine = false;
                queueNewLine = true;
                continue;
            }

            if (parseMarkdown) {

                //Handle Headline
                if ((c == '#') && isStartOfLine && (builder.codeBlockContext == null)) {
                    if (builder.headlineType == HeadlineType.NONE) {
                        if (StringUtils.startsWith(subText, "# ")) {
                            builder.headlineType = HeadlineType.BIGGEST;
                            charsToSkip = 1;
                        }
                        if (StringUtils.startsWith(subText, "## ")) {
                            builder.headlineType = HeadlineType.BIGGER;
                            charsToSkip = 2;
                        }
                        if (StringUtils.startsWith(subText, "### ")) {
                            builder.headlineType = HeadlineType.BIG;
                            charsToSkip = 3;
                        }
                        if (builder.headlineType != HeadlineType.NONE) {
                            continue;
                        }
                    }
                }

                //Handle Fonts
                if (StringUtils.startsWith(subLine, "%!!") && !StringUtils.startsWith(subLine, "%!!%") && StringUtils.contains(StringUtils.substring(subLine, 3), "%") && (builder.font == null) && (builder.codeBlockContext == null)) {
                    String fontName = StringUtils.split(StringUtils.substring(subLine, 3), "%", 2)[0];
                    if (RESOURCE_NAME_FILTER.isAllowedText(fontName)) {
                        if (StringUtils.contains(subText, "%!!%")) {
                            ResourceLocation font = null;
                            try {
                                font = new ResourceLocation(fontName);
                            } catch (Exception ignore) {}
                            if (font != null) {
                                if (!builder.text.isEmpty() || isStartOfLine) {
                                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                }
                                builder.font = font;
                                charsToSkip = fontName.length()+3;
                                continue;
                            }
                        }
                    }
                }
                if (StringUtils.startsWith(subText, "%!!%") && (builder.font != null)) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.font = null;
                    charsToSkip = 3;
                    continue;
                }

                //Handle HEX Coloring
                if (StringUtils.startsWith(subLine, "%#") && !StringUtils.startsWith(subLine, "%#%") && (builder.textColor == null) && (builder.codeBlockContext == null)) {
                    String s = (subLine.length() >= 11) ? StringUtils.substring(subLine, 1, 11) : "";
                    if (!StringUtils.endsWith(s, "%")) {
                        s = (subLine.length() >= 9) ? StringUtils.substring(subLine, 1, 9) : "";
                    }
                    if (StringUtils.endsWith(s, "%") && StringUtils.contains(subText, "%#%")) {
                        DrawableColor color = DrawableColor.of(StringUtils.substring(s, 0, s.length()-1));
                        if (color != DrawableColor.EMPTY) {
                            if (!builder.text.isEmpty() || isStartOfLine) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.textColor = color;
                            charsToSkip = s.length();
                            continue;
                        }
                    }
                }
                if (StringUtils.startsWith(subText, "%#%") && (builder.textColor != null)) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.textColor = null;
                    charsToSkip = 2;
                    continue;
                }

                //Handle Bold
                if ((c == '*') && !builder.bold && (builder.codeBlockContext == null)) {
                    String s2 = StringUtils.substring(markdownText, Math.min(markdownText.length(), index+2));
                    if (StringUtils.startsWith(subText, "**") && StringUtils.contains(s2, "**")) {
                        if (!builder.text.isEmpty() || isStartOfLine) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        }
                        builder.bold = true;
                        charsToSkip = 1;
                        continue;
                    }
                }
                if ((c == '*') && builder.bold) {
                    if (StringUtils.startsWith(subText, "**")) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.bold = false;
                        charsToSkip = 1;
                        continue;
                    }
                }

                int min1 = Math.min(markdownText.length(), index + 1);

                //Handle Italic Underscore
                if ((c == '_') && !builder.italic && (builder.codeBlockContext == null)) {
                    String s = StringUtils.substring(markdownText, min1);
                    if (StringUtils.contains(s, "_")) {
                        if (!builder.text.isEmpty() || isStartOfLine) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        }
                        builder.italic = true;
                        italicUnderscore = true;
                        continue;
                    }
                }
                if ((c == '_') && builder.italic && italicUnderscore) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.italic = false;
                    italicUnderscore = false;
                    continue;
                }

                //Handle Italic Asterisk
                if ((c == '*') && !builder.italic && (builder.codeBlockContext == null)) {
                    String s2 = StringUtils.substring(markdownText, min1);
                    if (!StringUtils.startsWith(subText, "**") && StringUtils.contains(s2, "*")) {
                        boolean isEndSingleAsterisk = false;
                        int index2 = 0;
                        for (char c2 : s2.toCharArray()) {
                            if (c2 == '*') {
                                String s3 = StringUtils.substring(s2, index2);
                                if (!StringUtils.startsWith(s3, "**")) {
                                    isEndSingleAsterisk = true;
                                    break;
                                }
                            }
                            index2++;
                        }
                        if (isEndSingleAsterisk) {
                            if (!builder.text.isEmpty() || isStartOfLine) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.italic = true;
                            continue;
                        }
                    }
                }
                if ((c == '*') && builder.italic && !italicUnderscore) {
                    if (!StringUtils.startsWith(subText, "**")) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.italic = false;
                        continue;
                    }
                }

                //Handle Strikethrough
                if ((c == '~') && !builder.strikethrough && (builder.codeBlockContext == null)) {
                    String s = StringUtils.substring(markdownText, min1);
                    if (StringUtils.contains(s, "~")) {
                        if (!builder.text.isEmpty() || isStartOfLine) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        }
                        builder.strikethrough = true;
                        continue;
                    }
                }
                if ((c == '~') && builder.strikethrough) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.strikethrough = false;
                    continue;
                }

                //Handle Hyperlink Image
                if ((c == '[') && isStartOfLine && (builder.codeBlockContext == null)) {
                    if (StringUtils.startsWith(currentLine, "[![")) {
                        List<String> hyperlinkImage = getHyperlinkImageFromLine(currentLine);
                        if (hyperlinkImage != null) {
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlinkImage.get(1);
                            setImageToBuilder(builder, hyperlinkImage.get(0));
                            lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                            builder.imageSupplier = null;
                            builder.hyperlink = null;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Image
                if ((c == '!') && isStartOfLine && (builder.codeBlockContext == null)) {
                    if (StringUtils.startsWith(currentLine, "![")) {
                        String imageLink = getImageFromLine(currentLine);
                        if (imageLink != null) {
                            setImageToBuilder(builder, imageLink);
                            lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                            builder.imageSupplier = null;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Hyperlink
                if ((c == '[') && (builder.hyperlink == null) && (builder.codeBlockContext == null)) {
                    String s2 = StringUtils.substring(markdownText, min1);
                    if (StringUtils.contains(s2, "](") && StringUtils.contains(s2, ")")) {
                        String hyperlink = getHyperlinkFromLine(subText);
                        if (hyperlink != null) {
                            if (!builder.text.isEmpty() || isStartOfLine) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlink;
                            continue;
                        }
                    }
                }
                if ((c == ']') && (builder.hyperlink != null)) {
                    if (StringUtils.startsWith(subText, "](")) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        charsToSkip = 2 + builder.hyperlink.link.length();
                        builder.hyperlink = null;
                        continue;
                    }
                }

                //Handle Quote
                if ((c == '>') && isStartOfLine && (builder.quoteContext == null) && (builder.codeBlockContext == null)) {
                    if (StringUtils.startsWith(subText, "> ")) {
                        builder.quoteContext = new QuoteContext();
                        charsToSkip = 1;
                        continue;
                    }
                }
                if (isStartOfLine && (builder.quoteContext != null) && StringUtils.replace(currentLine, " ", "").isEmpty()) {
                    builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                    lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                    queueNewLine = true;
                    continue;
                }

                //Handle Bullet List Level 1
                if (StringUtils.startsWith(subLine, "- ") && isStartOfLine && !removeInString(subLine, "-", " ", "\n").isEmpty() && (builder.codeBlockContext == null)) {
                    builder.bulletListLevel = 1;
                    builder.bulletListItemStart = true;
                    charsToSkip = 1;
                    continue;
                }
                //Handle Bullet List Level 2
                if (StringUtils.startsWith(subLine, "  - ") && isStartOfLine && !removeInString(subLine, "-", " ", "\n").isEmpty() && (builder.codeBlockContext == null)) {
                    builder.bulletListLevel = 2;
                    builder.bulletListItemStart = true;
                    charsToSkip = 3;
                    continue;
                }
                //TODO add more bullet list levels (make better handling for infinite levels)

                //Handle Separation Line
                if ((c == '-') && isStartOfLine && (builder.codeBlockContext == null)) {
                    CharacterFilter filter = new CharacterFilter();
                    filter.addAllowedCharacters('-');
                    String line = StringUtils.replace(currentLine, " ", "");
                    if (StringUtils.startsWith(subText, "---") && filter.isAllowedText(line)) {
                        builder.separationLine = true;
                        builder.text = new StringBuilder("---");
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        skipLine = true;
                        continue;
                    }
                }

                //Handle Code Block Single Line
                if ((c == '`') && (builder.codeBlockContext == null)) {
                    if (!StringUtils.startsWith(subText, "```") && isFormattedBlock(subText, '`', true)) {
                        if (!builder.text.isEmpty() || isStartOfLine) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        }
                        builder.codeBlockContext = new CodeBlockContext();
                        builder.codeBlockContext.singleLine = true;
                        continue;
                    }
                }
                if ((c == '`') && (builder.codeBlockContext != null) && builder.codeBlockContext.singleLine) {
                    if (!StringUtils.startsWith(subText, "```")) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.codeBlockContext = null;
                        continue;
                    }
                }

                //Handle Code Block Multi Line
                if ((c == '`') && isStartOfLine && (builder.codeBlockContext == null)) {
                    if (StringUtils.startsWith(subText, "```") && isFormattedBlock(subText, '`', false)) {
                        builder.codeBlockContext = new CodeBlockContext();
                        builder.codeBlockContext.singleLine = false;
                        skipLine = true;
                        continue;
                    }
                }
                if ((c == '`') && isStartOfLine && (builder.codeBlockContext != null) && !builder.codeBlockContext.singleLine) {
                    if (StringUtils.startsWith(subText, "```")) {
                        builder.codeBlockContext = null;
                        skipLine = true;
                        continue;
                    }
                }

                //Handle Alignment : Centered
                if (StringUtils.startsWith(subLine, "^^^") && !StringUtils.startsWith(subLine, "^^^^") && isStartOfLine && (builder.codeBlockContext == null)) {
                    if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, '^', false)) {
                        builder.alignment = MarkdownRenderer.MarkdownLineAlignment.CENTERED;
                        skipLine = true;
                        continue;
                    }
                    if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) && removeInString(subLine, "^", " ", "\n").isEmpty()) {
                        builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                        skipLine = true;
                        continue;
                    }
                }

                //Handle Alignment : Right
                if (StringUtils.startsWith(subLine, "|||") && !StringUtils.startsWith(subLine, "||||") && isStartOfLine && (builder.codeBlockContext == null)) {
                    if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, '|', false)) {
                        builder.alignment = MarkdownRenderer.MarkdownLineAlignment.RIGHT;
                        skipLine = true;
                        continue;
                    }
                    if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) && removeInString(subLine, "|", " ", "\n").isEmpty()) {
                        builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                        skipLine = true;
                        continue;
                    }
                }

            }

            if (c != '\n') {
                builder.text.append(c);
            }

            //Build fragment at every space
            if (c == ' ') {
                //Fix end-of-word of last fragment if needed
                if ((lastBuiltFragment != null) && !isStartOfLine && builder.text.toString().equals(" ")) {
                    lastBuiltFragment.endOfWord = true;
                    lastBuiltFragment.text += " ";
                    lastBuiltFragment.updateWidth();
                    builder.clearText();
                } else {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, true));
                }
                continue;
            }

            //Build fragment at end of line
            if (c == '\n') {
                lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                builder.bulletListLevel = 0;
                queueNewLine = true;
            }

        }

        //Manually build the last fragment of the last line, because it doesn't end with "\n"
        fragments.add(builder.build(true, true));

        return fragments;

    }

    protected static String removeInString(@NotNull String in, String... remove) {
        for (String s : remove) {
            in = StringUtils.replace(in, s, "");
        }
        return in;
    }

    protected static MarkdownTextFragment addFragment(List<MarkdownTextFragment> fragments, MarkdownTextFragment fragment) {
        fragments.add(fragment);
        return fragment;
    }

    protected static void setImageToBuilder(@NotNull FragmentBuilder builder, @NotNull String imageSource) {
        builder.imageSupplier = ResourceSupplier.image(imageSource);
    }

    @NotNull
    protected static String getLine(@NotNull String text) {
        return StringUtils.contains(text, "\n") ? StringUtils.split(text, "\n", 2)[0] : text;
    }

    protected static boolean isFormattedBlock(String text, char formatChar, boolean singleLine) {
        String longFormatCode = "" + formatChar + formatChar + formatChar;
        if (singleLine) {
            if (StringUtils.startsWith(text, "" + formatChar) && !StringUtils.startsWith(text, longFormatCode)) {
                int i = -1;
                boolean endFound = false;
                for (char c : text.toCharArray()) {
                    i++;
                    if (i == 0) {
                        continue;
                    }
                    if (c == '\n') {
                        break;
                    }
                    if (c == formatChar) {
                        if (!StringUtils.startsWith(StringUtils.substring(text, i), "" + formatChar + formatChar + formatChar)) {
                            endFound = true;
                            break;
                        }
                    }
                }
                return endFound;
            }
        } else {
            if (StringUtils.startsWith(text, longFormatCode)) {
                int i = -1;
                boolean endFound = false;
                boolean newLine = false;
                for (char c : text.toCharArray()) {
                    i++;
                    if (i < 3) {
                        continue;
                    }
                    if ((c == formatChar) && newLine && StringUtils.startsWith(StringUtils.substring(text, i), longFormatCode)) {
                        endFound = true;
                        break;
                    }
                    if (newLine) {
                        newLine = false;
                    }
                    if (c == '\n') {
                        newLine = true;
                    }
                }
                return endFound;
            }
        }
        return false;
    }

    @Nullable
    protected static List<String> getHyperlinkImageFromLine(String line) {
        if (StringUtils.startsWith(line, "[![") && StringUtils.contains(line, "](") && StringUtils.contains(line, ")")) {
            String imageLink = null;
            String hyperLink = null;
            int index = -1;
            for (char ignored : line.toCharArray()) {
                index++;
                String sub = StringUtils.substring(line, index);
                if (index < 1) {
                    continue;
                }
                if (StringUtils.startsWith(sub, "![")) {
                    imageLink = getImageFromLine(sub);
                    if (imageLink != null) {
                        String s = StringUtils.split(sub, "[)]", 2)[0] + ")";
                        hyperLink = getHyperlinkFromLine(line.replace(s, ""));
                        break;
                    }
                }
            }
            if ((imageLink != null) && (hyperLink != null)) {
                return ListUtils.of(imageLink, hyperLink);
            }
        }
        return null;
    }

    @Nullable
    protected static String getImageFromLine(String line) {
        if (StringUtils.startsWith(line, "![") && StringUtils.contains(line, "](") && StringUtils.contains(line, ")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideImageLink = false;
            StringBuilder imageLink = new StringBuilder();
            boolean openRoundBracketsFound = false;
            String s = StringUtils.substring(line, 2);
            int index = -1;
            for (char c : s.toCharArray()) {
                index++;
                if (c == '\n') {
                    return null;
                }
                if (c == ']') {
                    if (!beforeClosedBrackets) {
                        return null;
                    }
                    beforeClosedBrackets = false;
                    if (StringUtils.startsWith(StringUtils.substring(s, index), "](")) {
                        isInsideImageLink = true;
                        continue;
                    }
                }
                if ((c == '[') && beforeClosedBrackets) {
                    return null;
                }
                if (isInsideImageLink) {
                    if (c == '(') {
                        if (!openRoundBracketsFound) {
                            openRoundBracketsFound = true;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    if (c == ')') {
                        return imageLink.toString();
                    }
                    imageLink.append(c);
                }
            }
        }
        return null;
    }

    @Nullable
    protected static String getHyperlinkFromLine(String line) {
        if (StringUtils.startsWith(line, "[") && StringUtils.contains(line, "](") && StringUtils.contains(line, ")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideHyperlink = false;
            StringBuilder hyperlink = new StringBuilder();
            boolean openRoundBracketsFound = false;
            String s = StringUtils.substring(line, 1);
            int index = -1;
            for (char c : s.toCharArray()) {
                index++;
                if (c == '\n') {
                    return null;
                }
                if (c == ']') {
                    if (!beforeClosedBrackets) {
                        return null;
                    }
                    beforeClosedBrackets = false;
                    if (StringUtils.startsWith(StringUtils.substring(s, index), "](")) {
                        isInsideHyperlink = true;
                        continue;
                    }
                }
                if ((c == '[') && beforeClosedBrackets) {
                    return null;
                }
                if (isInsideHyperlink) {
                    if (c == '(') {
                        if (!openRoundBracketsFound) {
                            openRoundBracketsFound = true;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    if (c == ')') {
                        return hyperlink.toString();
                    }
                    hyperlink.append(c);
                }
            }
        }
        return null;
    }

    protected static class FragmentBuilder {

        protected final MarkdownRenderer renderer;
        protected StringBuilder text = new StringBuilder();
        protected DrawableColor textColor = null;
        protected ResourceSupplier<ITexture> imageSupplier = null;
        protected boolean separationLine = false;
        protected boolean bold = false;
        protected boolean italic = false;
        protected boolean strikethrough = false;
        protected boolean bulletListItemStart = false;
        protected int bulletListLevel = 0;
        @NotNull
        protected MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
        protected Hyperlink hyperlink = null;
        @NotNull
        protected HeadlineType headlineType = HeadlineType.NONE;
        protected QuoteContext quoteContext = null;
        protected CodeBlockContext codeBlockContext = null;
        protected ResourceLocation font = null;

        protected FragmentBuilder(MarkdownRenderer renderer) {
            this.renderer = renderer;
        }

        @NotNull
        protected MarkdownTextFragment build(boolean naturalLineBreakAfter, boolean endOfWord) {
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, text.toString());
            frag.font = font;
            frag.textColor = textColor;
            frag.imageSupplier = imageSupplier;
            frag.separationLine = separationLine;
            if (separationLine) {
                frag.unscaledTextHeight = 1;
            }
            frag.bold = bold;
            frag.italic = italic;
            frag.strikethrough = strikethrough;
            frag.bulletListLevel = bulletListLevel;
            frag.bulletListItemStart = bulletListItemStart;
            bulletListItemStart = false;
            frag.alignment = alignment;
            frag.hyperlink = hyperlink;
            if (hyperlink != null) {
                hyperlink.hyperlinkFragments.add(frag);
            }
            frag.headlineType = headlineType;
            frag.quoteContext = quoteContext;
            if (quoteContext != null) {
                quoteContext.quoteFragments.add(frag);
            }
            frag.codeBlockContext = codeBlockContext;
            if (codeBlockContext != null) {
                codeBlockContext.codeBlockFragments.add(frag);
            }
            frag.naturalLineBreakAfter = naturalLineBreakAfter;
            frag.endOfWord = endOfWord;
            frag.updateWidth();
            this.clearText();
            return frag;
        }

        protected FragmentBuilder clearText() {
            this.text = new StringBuilder();
            return this;
        }

    }

}
