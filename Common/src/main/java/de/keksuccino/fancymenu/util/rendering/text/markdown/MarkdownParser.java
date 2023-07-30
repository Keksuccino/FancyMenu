package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
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
        boolean startOfLine = true;
        boolean italicUnderscore = false;
        int charsToSkip = 0;

        int index = -1;
        for (char c : markdownText.toCharArray()) {

            boolean cachedStartOfLine = startOfLine;
            startOfLine = false;

            index++;

            if (charsToSkip > 0) {
                charsToSkip--;
                continue;
            }

            //Handle Headline
            if ((c == '#') && cachedStartOfLine) {
                if (builder.headlineType == HeadlineType.NONE) {
                    String s = markdownText.substring(index);
                    if (s.startsWith("# ")) {
                        builder.headlineType = HeadlineType.BIGGEST;
                        charsToSkip = 2;
                    }
                    if (s.startsWith("## ")) {
                        builder.headlineType = HeadlineType.BIGGER;
                        charsToSkip = 3;
                    }
                    if (s.startsWith("### ")) {
                        builder.headlineType = HeadlineType.BIG;
                        charsToSkip = 4;
                    }
                }
            }

            //Handle Bold
            if ((c == '*') && !builder.bold) {
                String s = markdownText.substring(index);
                String s2 = markdownText.substring(Math.min(markdownText.length(), index+2));
                if (s.startsWith("**") && s2.contains("**")) {
                    fragments.add(builder.build(false));
                    builder.bold = true;
                    charsToSkip = 2;
                    continue;
                }
            }
            if ((c == '*') && builder.bold) {
                String s = markdownText.substring(index);
                if (s.startsWith("**")) {
                    fragments.add(builder.build(false));
                    builder.bold = false;
                    charsToSkip = 2;
                    continue;
                }
            }

            //Handle Italic Underscore
            if ((c == '_') && !builder.italic) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("_")) {
                    fragments.add(builder.build(false));
                    builder.italic = true;
                    italicUnderscore = true;
                    charsToSkip = 1;
                    continue;
                }
            }
            if ((c == '_') && builder.italic && italicUnderscore) {
                fragments.add(builder.build(false));
                builder.italic = false;
                italicUnderscore = false;
                charsToSkip = 1;
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
                        fragments.add(builder.build(false));
                        builder.italic = true;
                        charsToSkip = 1;
                        continue;
                    }
                }
            }
            if ((c == '*') && builder.italic && !italicUnderscore) {
                String s = markdownText.substring(index);
                if (!s.startsWith("**")) {
                    fragments.add(builder.build(false));
                    builder.italic = false;
                    charsToSkip = 1;
                    continue;
                }
            }

            //Handle Strikethrough
            if ((c == '~') && !builder.strikethrough) {
                String s = markdownText.substring(Math.min(markdownText.length(), index+1));
                if (s.contains("~")) {
                    fragments.add(builder.build(false));
                    builder.strikethrough = true;
                    charsToSkip = 1;
                    continue;
                }
            }
            if ((c == '~') && builder.strikethrough) {
                fragments.add(builder.build(false));
                builder.strikethrough = false;
                charsToSkip = 1;
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
                            fragments.add(builder.build(false));
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
                    fragments.add(builder.build(false));
                    charsToSkip = 3 + builder.hyperlink.link.length();
                    builder.hyperlink = null;
                    continue;
                }
            }

            builder.text += c;

            //Build fragment at every space
            if (c == ' ') {
                fragments.add(builder.build(false));
                continue;
            }

            //Build fragment at end of line
            if (c == '\n') {
                fragments.add(builder.build(true));
                builder.headlineType = HeadlineType.NONE;
                startOfLine = true;
            }

        }
        
        return fragments;
        
    }

    @Nullable
    protected static String getHyperlinkFrom(String textFragment) {
        if (textFragment.startsWith("[") && textFragment.contains("](") && textFragment.contains(")")) {
            boolean beforeClosedBrackets = true;
            boolean isInsideHyperlink = false;
            String hyperlink = "";
            boolean openRoundBracketsFound = false;
            String s = textFragment.substring(1);
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
        protected boolean bold = false;
        protected boolean italic = false;
        protected boolean strikethrough = false;
        protected Hyperlink hyperlink = null;
        protected HeadlineType headlineType = HeadlineType.NONE;
        protected boolean quote = false;
        protected boolean quoteStart = false;

        protected FragmentBuilder(MarkdownRenderer renderer) {
            this.renderer = renderer;
        }

        @NotNull
        protected MarkdownTextFragment build(boolean naturalLineBreakAfter) {
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, text);
            frag.bold = bold;
            frag.italic = italic;
            frag.strikethrough = strikethrough;
            frag.hyperlink = hyperlink;
            if (hyperlink != null) {
                hyperlink.hyperlinkFragments.add(frag);
            }
            frag.headlineType = headlineType;
            frag.quote = quote;
            frag.quoteStart = quoteStart;
            frag.naturalLineBreakAfter = naturalLineBreakAfter;
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
