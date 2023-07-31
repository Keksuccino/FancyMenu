package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownParser {

    @SuppressWarnings("all")
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

        String currentLine = "";

        int index = -1;
        for (char c : markdownText.toCharArray()) {

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            index++;

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
            if ((c == '#') && isStartOfLine) {
                if (builder.headlineType == HeadlineType.NONE) {
                    String s = markdownText.substring(index);
                    if (s.startsWith("# ")) {
                        builder.headlineType = HeadlineType.BIGGEST;
                        charsToSkip = 1;
                    }
                    if (s.startsWith("## ")) {
                        builder.headlineType = HeadlineType.BIGGER;
                        charsToSkip = 2;
                    }
                    if (s.startsWith("### ")) {
                        builder.headlineType = HeadlineType.BIG;
                        charsToSkip = 3;
                    }
                    if (builder.headlineType != HeadlineType.NONE) {
                        continue;
                    }
                }
            }

            //Handle Bold
            if ((c == '*') && !builder.bold) {
                String s = markdownText.substring(index);
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+2));
                if (s.startsWith("**") && s2.contains("**")) {
                    fragments.add(builder.build(false, false));
                    builder.bold = true;
                    charsToSkip = 1;
                    continue;
                }
            }
            if ((c == '*') && builder.bold) {
                String s = markdownText.substring(index);
                if (s.startsWith("**")) {
                    fragments.add(builder.build(false, false));
                    builder.bold = false;
                    charsToSkip = 1;
                    continue;
                }
            }

            //Handle Italic Underscore
            if ((c == '_') && !builder.italic) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("_")) {
                    fragments.add(builder.build(false, false));
                    builder.italic = true;
                    italicUnderscore = true;
                    continue;
                }
            }
            if ((c == '_') && builder.italic && italicUnderscore) {
                fragments.add(builder.build(false, false));
                builder.italic = false;
                italicUnderscore = false;
                continue;
            }

            //Handle Italic Asterisk
            if ((c == '*') && !builder.italic) {
                String s = markdownText.substring(index);
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (!s.startsWith("**") && s2.contains("*")) {
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
                        fragments.add(builder.build(false, false));
                        builder.italic = true;
                        continue;
                    }
                }
            }
            if ((c == '*') && builder.italic && !italicUnderscore) {
                String s = markdownText.substring(index);
                if (!s.startsWith("**")) {
                    fragments.add(builder.build(false, false));
                    builder.italic = false;
                    continue;
                }
            }

            //Handle Strikethrough
            if ((c == '~') && !builder.strikethrough) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("~")) {
                    fragments.add(builder.build(false, false));
                    builder.strikethrough = true;
                    continue;
                }
            }
            if ((c == '~') && builder.strikethrough) {
                fragments.add(builder.build(false, false));
                builder.strikethrough = false;
                continue;
            }

            //Handle Hyperlink
            if (c == '[') {
                if (builder.hyperlink == null) {
                    String s = markdownText.substring(index);
                    String s2 = markdownText.substring(Math.min(markdownText.length(), index+1));
                    if (s2.contains("](") && s2.contains(")")) {
                        String hyperlink = getHyperlinkFrom(s);
                        if (hyperlink != null) {
                            fragments.add(builder.build(false, false));
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlink;
                            continue;
                        }
                    }
                }
            }
            if ((c == ']') && (builder.hyperlink != null)) {
                String s = markdownText.substring(index);
                if (s.startsWith("](")) {
                    fragments.add(builder.build(false, false));
                    charsToSkip = 2 + builder.hyperlink.link.length();
                    builder.hyperlink = null;
                    continue;
                }
            }

            //Handle Quote
            if ((c == '>') && isStartOfLine && (builder.quoteContext == null)) {
                String s = markdownText.substring(index);
                if (s.startsWith("> ")) {
                    builder.quoteContext = new QuoteContext();
                    charsToSkip = 1;
                    continue;
                }
            }
            if (isStartOfLine && (builder.quoteContext != null) && currentLine.replace(" ", "").isEmpty()) {
                builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                fragments.add(builder.build(true, true));
                queueNewLine = true;
                continue;
            }

            //Handle Separation Line
            if ((c == '-') && isStartOfLine) {
                String s = markdownText.substring(index);
                CharacterFilter filter = new CharacterFilter();
                filter.addAllowedCharacters('-');
                String line = currentLine.replace(" ", "");
                if (s.startsWith("---") && filter.isAllowedText(line)) {
                    builder.separationLine = true;
                    builder.text = "---";
                    fragments.add(builder.build(true, true));
                    skipLine = true;
                    continue;
                }
            }

            if (c != '\n') {
                builder.text += c;
            }

            //Build fragment at every space
            if (c == ' ') {
                fragments.add(builder.build(false, true));
                continue;
            }

            //Build fragment at end of line
            if (c == '\n') {
                fragments.add(builder.build(true, true));
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                queueNewLine = true;
            }

        }
        
        return fragments;
        
    }

    @NotNull
    protected static String getLine(@NotNull String text) {
        return text.contains("\n") ? text.split("\n", 2)[0] : text;
    }

    @Nullable
    protected static String getHyperlinkFrom(String text) {
        if (text.startsWith("[") && text.contains("](") && text.contains(")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideHyperlink = false;
            String hyperlink = "";
            boolean openRoundBracketsFound = false;
            String s = text.substring(1);
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
        protected boolean separationLine = false;
        protected boolean bold = false;
        protected boolean italic = false;
        protected boolean strikethrough = false;
        protected Hyperlink hyperlink = null;
        protected HeadlineType headlineType = HeadlineType.NONE;
        protected QuoteContext quoteContext = null;

        protected FragmentBuilder(MarkdownRenderer renderer) {
            this.renderer = renderer;
        }

        @NotNull
        protected MarkdownTextFragment build(boolean naturalLineBreakAfter, boolean endOfWord) {
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, text);
            frag.separationLine = separationLine;
            frag.bold = bold;
            frag.italic = italic;
            frag.strikethrough = strikethrough;
            frag.hyperlink = hyperlink;
            if (hyperlink != null) {
                hyperlink.hyperlinkFragments.add(frag);
            }
            frag.headlineType = headlineType;
            frag.quoteContext = quoteContext;
            if (quoteContext != null) {
                quoteContext.quoteFragments.add(frag);
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
