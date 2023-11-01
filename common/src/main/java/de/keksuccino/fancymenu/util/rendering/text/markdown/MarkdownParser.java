package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
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

    @NotNull
    public static List<MarkdownTextFragment> parse(@NotNull MarkdownRenderer renderer, @NotNull String markdownText) {

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
            String subText = markdownText.substring(index);
            //Part of current line, starting at current index
            String subLine = getLine(subText);

            //Update Current Line
            if (isStartOfLine) {
                currentLine = getLine(markdownText.substring(index));
            }

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

            //Handle Headline
            if ((c == '#') && isStartOfLine && (builder.codeBlockContext == null)) {
                if (builder.headlineType == HeadlineType.NONE) {
                    if (subText.startsWith("# ")) {
                        builder.headlineType = HeadlineType.BIGGEST;
                        charsToSkip = 1;
                    }
                    if (subText.startsWith("## ")) {
                        builder.headlineType = HeadlineType.BIGGER;
                        charsToSkip = 2;
                    }
                    if (subText.startsWith("### ")) {
                        builder.headlineType = HeadlineType.BIG;
                        charsToSkip = 3;
                    }
                    if (builder.headlineType != HeadlineType.NONE) {
                        continue;
                    }
                }
            }

            //Handle Fonts
            if (subLine.startsWith("%!!") && !subLine.startsWith("%!!%") && subLine.substring(3).contains("%") && (builder.font == null) && (builder.codeBlockContext == null)) {
                String fontName = subLine.substring(3).split("%", 2)[0];
                if (RESOURCE_NAME_FILTER.isAllowedText(fontName)) {
                    if (subText.contains("%!!%")) {
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
            if (subText.startsWith("%!!%") && (builder.font != null)) {
                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                builder.font = null;
                charsToSkip = 3;
                continue;
            }

            //Handle HEX Coloring
            if (subLine.startsWith("%#") && !subLine.startsWith("%#%") && (builder.textColor == null) && (builder.codeBlockContext == null)) {
                String s = (subLine.length() >= 11) ? subLine.substring(1, 11) : "";
                if (!s.endsWith("%")) {
                    s = (subLine.length() >= 9) ? subLine.substring(1, 9) : "";
                }
                if (s.endsWith("%") && subText.contains("%#%")) {
                    DrawableColor color = DrawableColor.of(s.substring(0, s.length()-1));
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
            if (subText.startsWith("%#%") && (builder.textColor != null)) {
                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                builder.textColor = null;
                charsToSkip = 2;
                continue;
            }

            //Handle Bold
            if ((c == '*') && !builder.bold && (builder.codeBlockContext == null)) {
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+2));
                if (subText.startsWith("**") && s2.contains("**")) {
                    if (!builder.text.isEmpty() || isStartOfLine) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    }
                    builder.bold = true;
                    charsToSkip = 1;
                    continue;
                }
            }
            if ((c == '*') && builder.bold) {
                if (subText.startsWith("**")) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.bold = false;
                    charsToSkip = 1;
                    continue;
                }
            }

            //Handle Italic Underscore
            if ((c == '_') && !builder.italic && (builder.codeBlockContext == null)) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("_")) {
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
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (!subText.startsWith("**") && s2.contains("*")) {
                    boolean isEndSingleAsterisk = false;
                    int index2 = 0;
                    for (char c2 : s2.toCharArray()) {
                        if (c2 == '*') {
                            String s3 = s2.substring(index2);
                            if (!s3.startsWith("**")) {
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
                if (!subText.startsWith("**")) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.italic = false;
                    continue;
                }
            }

            //Handle Strikethrough
            if ((c == '~') && !builder.strikethrough && (builder.codeBlockContext == null)) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("~")) {
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
                if (currentLine.startsWith("[![")) {
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
                if (currentLine.startsWith("![")) {
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
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s2.contains("](") && s2.contains(")")) {
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
                if (subText.startsWith("](")) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    charsToSkip = 2 + builder.hyperlink.link.length();
                    builder.hyperlink = null;
                    continue;
                }
            }

            //Handle Quote
            if ((c == '>') && isStartOfLine && (builder.quoteContext == null) && (builder.codeBlockContext == null)) {
                if (subText.startsWith("> ")) {
                    builder.quoteContext = new QuoteContext();
                    charsToSkip = 1;
                    continue;
                }
            }
            if (isStartOfLine && (builder.quoteContext != null) && currentLine.replace(" ", "").isEmpty()) {
                builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                queueNewLine = true;
                continue;
            }

            //Handle Bullet List Level 1
            if (subLine.startsWith("- ") && isStartOfLine && !subLine.replace("-", "").replace(" ", "").replace("\n", "").isEmpty() && (builder.codeBlockContext == null)) {
                builder.bulletListLevel = 1;
                builder.bulletListItemStart = true;
                charsToSkip = 1;
                continue;
            }
            //Handle Bullet List Level 2
            if (subLine.startsWith("  - ") && isStartOfLine && !subLine.replace("-", "").replace(" ", "").replace("\n", "").isEmpty() && (builder.codeBlockContext == null)) {
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
                String line = currentLine.replace(" ", "");
                if (subText.startsWith("---") && filter.isAllowedText(line)) {
                    builder.separationLine = true;
                    builder.text = "---";
                    lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                    skipLine = true;
                    continue;
                }
            }

            //Handle Code Block Single Line
            if ((c == '`') && (builder.codeBlockContext == null)) {
                if (!subText.startsWith("```") && isFormattedBlock(subText, '`', true)) {
                    if (!builder.text.isEmpty() || isStartOfLine) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    }
                    builder.codeBlockContext = new CodeBlockContext();
                    builder.codeBlockContext.singleLine = true;
                    continue;
                }
            }
            if ((c == '`') && (builder.codeBlockContext != null) && builder.codeBlockContext.singleLine) {
                if (!subText.startsWith("```")) {
                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                    builder.codeBlockContext = null;
                    continue;
                }
            }

            //Handle Code Block Multi Line
            if ((c == '`') && isStartOfLine && (builder.codeBlockContext == null)) {
                if (subText.startsWith("```") && isFormattedBlock(subText, '`', false)) {
                    builder.codeBlockContext = new CodeBlockContext();
                    builder.codeBlockContext.singleLine = false;
                    skipLine = true;
                    continue;
                }
            }
            if ((c == '`') && isStartOfLine && (builder.codeBlockContext != null) && !builder.codeBlockContext.singleLine) {
                if (subText.startsWith("```")) {
                    builder.codeBlockContext = null;
                    skipLine = true;
                    continue;
                }
            }

            //Handle Alignment : Centered
            if (subLine.startsWith("^^^") && !subLine.startsWith("^^^^") && isStartOfLine && (builder.codeBlockContext == null)) {
                if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, '^', false)) {
                    builder.alignment = MarkdownRenderer.MarkdownLineAlignment.CENTERED;
                    skipLine = true;
                    continue;
                }
                if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) && subLine.replace("^", "").replace(" ", "").replace("\n", "").isEmpty()) {
                    builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                    skipLine = true;
                    continue;
                }
            }

            //Handle Alignment : Right
            if (subLine.startsWith("|||") && !subLine.startsWith("||||") && isStartOfLine && (builder.codeBlockContext == null)) {
                if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, '|', false)) {
                    builder.alignment = MarkdownRenderer.MarkdownLineAlignment.RIGHT;
                    skipLine = true;
                    continue;
                }
                if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) && subLine.replace("|", "").replace(" ", "").replace("\n", "").isEmpty()) {
                    builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                    skipLine = true;
                    continue;
                }
            }

            if (c != '\n') {
                builder.text += c;
            }

            //Build fragment at every space
            if (c == ' ') {
                //Fix end-of-word of last fragment if needed
                if ((lastBuiltFragment != null) && !isStartOfLine && builder.text.equals(" ")) {
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

    protected static MarkdownTextFragment addFragment(List<MarkdownTextFragment> fragments, MarkdownTextFragment fragment) {
        fragments.add(fragment);
        return fragment;
    }

    protected static void setImageToBuilder(@NotNull FragmentBuilder builder, @NotNull String imageSource) {
//        if (TextValidators.BASIC_URL_TEXT_VALIDATOR.get(imageLink)) {
//            builder.image = ImageResourceHandler.INSTANCE.getWebTexture(imageLink);
//            builder.text = "Image";
//        } else {
//            String s = ScreenCustomization.getAbsoluteGameDirectoryPath(imageLink);
//            File f = new File(s);
//            if (f.isFile() && f.getPath().startsWith(LayoutHandler.ASSETS_DIR.getAbsolutePath()) && FileFilter.IMAGE_AND_GIF_FILE_FILTER.checkFile(f)) {
//                builder.image = ImageResourceHandler.INSTANCE.getTexture(f);
//                if (builder.image == null) {
//                    builder.text = I18n.get("fancymenu.markdown.missing_local_image");
//                } else {
//                    builder.text = "Image";
//                }
//            } else {
//                builder.text = I18n.get("fancymenu.markdown.missing_local_image");
//            }
//        }
        builder.imageSupplier = ResourceSupplier.image(imageSource);
    }

    @NotNull
    protected static String getLine(@NotNull String text) {
        return text.contains("\n") ? text.split("\n", 2)[0] : text;
    }

    protected static boolean isFormattedBlock(String text, char formatChar, boolean singleLine) {
        String longFormatCode = "" + formatChar + formatChar + formatChar;
        if (singleLine) {
            if (text.startsWith("" + formatChar) && !text.startsWith(longFormatCode)) {
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
                        if (!text.substring(i).startsWith("" + formatChar + formatChar + formatChar)) {
                            endFound = true;
                            break;
                        }
                    }
                }
                return endFound;
            }
        } else {
            if (text.startsWith(longFormatCode)) {
                int i = -1;
                boolean endFound = false;
                boolean newLine = false;
                for (char c : text.toCharArray()) {
                    i++;
                    if (i < 3) {
                        continue;
                    }
                    if ((c == formatChar) && newLine && text.substring(i).startsWith(longFormatCode)) {
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
        if (line.startsWith("[![") && line.contains("](") && line.contains(")")) {
            String imageLink = null;
            String hyperLink = null;
            int index = -1;
            for (char ignored : line.toCharArray()) {
                index++;
                String sub = line.substring(index);
                if (index < 1) {
                    continue;
                }
                if (sub.startsWith("![")) {
                    imageLink = getImageFromLine(sub);
                    if (imageLink != null) {
                        String s = sub.split("[)]", 2)[0] + ")";
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
        if (line.startsWith("![") && line.contains("](") && line.contains(")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideImageLink = false;
            String imageLink = "";
            boolean openRoundBracketsFound = false;
            String s = line.substring(2);
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
                    if (s.substring(index).startsWith("](")) {
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
                        return imageLink;
                    }
                    imageLink += c;
                }
            }
        }
        return null;
    }

    @Nullable
    protected static String getHyperlinkFromLine(String line) {
        if (line.startsWith("[") && line.contains("](") && line.contains(")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideHyperlink = false;
            String hyperlink = "";
            boolean openRoundBracketsFound = false;
            String s = line.substring(1);
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
                    if (s.substring(index).startsWith("](")) {
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
                        return hyperlink;
                    }
                    hyperlink += c;
                }
            }
        }
        return null;
    }

    protected static class FragmentBuilder {

        protected final MarkdownRenderer renderer;
        protected String text = "";
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
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, text);
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
            this.text = "";
            return this;
        }

    }

}
