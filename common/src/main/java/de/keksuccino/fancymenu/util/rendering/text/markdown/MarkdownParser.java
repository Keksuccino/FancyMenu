package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.CodeBlockContext;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.HeadlineType;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.Hyperlink;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTextFragment.QuoteContext;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String EMPTY_STRING = "";
    private static final char NEWLINE_CHAR = '\n';
    private static final String NEWLINE = "\n";
    private static final String HEADLINE_PREFIX_BIGGEST = "# ";
    private static final String HEADLINE_PREFIX_BIGGER = "## ";
    private static final String HEADLINE_PREFIX_BIG = "### ";
    private static final char HASHTAG_CHAR = '#';
    private static final String FORMATTING_CODE_FONT_PREFIX = "%!!";
    private static final String FORMATTING_CODE_FONT_SUFFIX = "%!!%";
    private static final String PERCENTAGE = "%";
    private static final char PERCENTAGE_CHAR = '%';
    private static final String FORMATTING_CODE_HEX_COLOR_PREFIX = "%#";
    private static final String FORMATTING_CODE_HEX_COLOR_SUFFIX = "%#%";
    private static final char ASTERISK_CHAR = '*';
    private static final String FORMATTING_CODE_BOLD_PREFIX_SUFFIX = "**";
    private static final char UNDERSCORE_CHAR = '_';
    private static final char BACKSLASH_CHAR = '\\';
    private static final char TILDE_CHAR = '~';
    private static final char EXCLAMATION_MARK_CHAR = '!';
    private static final String FORMATTING_CODE_IMAGE_PREFIX = "![";
    private static final String FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX = "[![";
    private static final String FORMATTING_CODE_HYPERLINK_INNER_PREFIX = "](";
    private static final char OPEN_ROUND_BRACKETS_CHAR = '(';
    private static final char CLOSE_ROUND_BRACKETS_CHAR = ')';
    private static final String CLOSE_ROUND_BRACKETS = ")";
    private static final char OPEN_SQUARE_BRACKETS_CHAR = '[';
    private static final char CLOSE_SQUARE_BRACKETS_CHAR = ']';
    private static final String OPEN_SQUARE_BRACKETS = "[";
    private static final char GREATER_THAN_CHAR = '>';
    private static final String FORMATTING_CODE_QUOTE_PREFIX = "> ";
    private static final String SPACE = " ";
    private static final char SPACE_CHAR = ' ';
    private static final String BULLET_LIST_PATTERN = "^( *)- ";
    private static final char MINUS_CHAR = '-';
    private static final char PLUS_CHAR = '+';
    private static final char DOT_CHAR = '.';
    private static final String FORMATTING_CODE_SEPARATION_LINE_PREFIX = "---";
    private static final char GRAVE_ACCENT_CHAR = '`';
    private static final String FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX = "```";
    private static final char SEMICOLON_CHAR = ';';
    private static final String SEMICOLON = ";";
    private static final String FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX = ";;";
    private static final String FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX = ";;;";
    private static final char CIRCUMFLEX_CHAR = '^';
    private static final String CIRCUMFLEX = "^";
    private static final String FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX = "^^^";
    private static final char VERTICAL_BAR_CHAR = '|';
    private static final String FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX = "|||";
    private static final String VERTICAL_BAR = "|";
    private static final char OPEN_CURLY_BRACKET_CHAR = '{';
    private static final char CLOSE_CURLY_BRACKET_CHAR = '}';
    private static final CharacterFilter RESOURCE_NAME_FILTER = CharacterFilter.buildResourceNameFilter();
    private static final CharacterFilter MINUS_CHARACTER_FILTER = ObjectUtils.build(() -> {
        CharacterFilter filter = new CharacterFilter();
        filter.addAllowedCharacters(MINUS_CHAR);
        return filter;
    });
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final String COLON = ":";

    @NotNull
    public static List<MarkdownTextFragment> parse(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, boolean parseMarkdown) {

        Objects.requireNonNull(renderer);
        Objects.requireNonNull(markdownText);

        List<MarkdownTextFragment> fragments = new ArrayList<>();
        List<MarkdownTextFragment.TableContext> tableContexts = new ArrayList<>();

        // Pre-process tables if markdown parsing is enabled
        if (parseMarkdown) {
            markdownText = preprocessTables(renderer, markdownText, tableContexts);
        }

        FragmentBuilder builder = new FragmentBuilder(renderer, parseMarkdown);
        boolean queueNewLine = true;
        boolean italicUnderscore = false;
        int charsToSkip = 0;
        boolean skipLine = false;
        MarkdownTextFragment lastBuiltFragment = null;
        boolean lastLineWasHeadline = false;

        String currentLine = EMPTY_STRING;
        int tableContextIndex = 0;

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

            // Check for table placeholder
            if (isStartOfLine && StringUtils.startsWith(currentLine, "[TABLE_")) {
                // Extract table index from placeholder
                String indexStr = StringUtils.substringBetween(currentLine, "[TABLE_", "]");
                if (indexStr != null) {
                    try {
                        int tableIndex = Integer.parseInt(indexStr);
                        if (tableIndex >= 0 && tableIndex < tableContexts.size()) {
                            // Create table fragment
                            MarkdownTextFragment tableFragment = new MarkdownTextFragment(renderer, "[TABLE]");
                            tableFragment.tableContext = tableContexts.get(tableIndex);
                            tableFragment.naturalLineBreakAfter = true;
                            // Inherit current alignment from builder
                            tableFragment.alignment = builder.alignment;
                            lastBuiltFragment = addFragment(fragments, tableFragment);
                            skipLine = true;
                            continue;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                // If we couldn't parse the table index, skip the line
                skipLine = true;
                continue;
            }

            //Skip empty line under headline
            if ((c == NEWLINE_CHAR) && lastLineWasHeadline) {
                String nextLine = null;
                try { nextLine = StringUtils.substring(subText, 1).split(NEWLINE, 2)[0]; } catch (Exception ignored) {}
                if ((nextLine != null) && removeFromString(nextLine, NEWLINE, SPACE).isEmpty()) {
                    lastLineWasHeadline = false;
                    continue;
                }
                lastLineWasHeadline = false;
            }

            //Skip Chars
            if (charsToSkip > 0) {
                charsToSkip--;
                continue;
            }

            //Skip Line
            if ((c != NEWLINE_CHAR) && skipLine) {
                continue;
            }
            if ((c == NEWLINE_CHAR) && skipLine) {
                builder.headlineType = HeadlineType.NONE;
                builder.separationLine = false;
                skipLine = false;
                queueNewLine = true;
                continue;
            }

            if (parseMarkdown) {

                //Handle Escapes
                if ((c == BACKSLASH_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    int nextIndex = index + 1;
                    if (nextIndex < markdownText.length()) {
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
                        if (StringUtils.startsWith(subText, HEADLINE_PREFIX_BIGGEST)) {
                            builder.headlineType = HeadlineType.BIGGEST;
                            charsToSkip = 1;
                        }
                        if (StringUtils.startsWith(subText, HEADLINE_PREFIX_BIGGER)) {
                            builder.headlineType = HeadlineType.BIGGER;
                            charsToSkip = 2;
                        }
                        if (StringUtils.startsWith(subText, HEADLINE_PREFIX_BIG)) {
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
                    if (StringUtils.startsWith(subLine, FORMATTING_CODE_FONT_PREFIX) && !StringUtils.startsWith(subLine, FORMATTING_CODE_FONT_SUFFIX) && (builder.font == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                        String afterPrefix = StringUtils.substring(subLine, 3);
                        if (StringUtils.contains(afterPrefix, PERCENTAGE_CHAR)) {
                            String fontName = StringUtils.split(afterPrefix, PERCENTAGE, 2)[0];
                            if (RESOURCE_NAME_FILTER.isAllowedText(fontName)) {
                                if (StringUtils.contains(subText, FORMATTING_CODE_FONT_SUFFIX)) {
                                    Identifier font = null;
                                    try {
                                        font = Identifier.parse(fontName);
                                    } catch (Exception ignore) {}
                                    if (font != null) {
                                        if (isStartOfLine || !builder.text.isEmpty()) {
                                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                        }
                                        builder.font = font;
                                        charsToSkip = fontName.length()+3;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    if (StringUtils.startsWith(subText, FORMATTING_CODE_FONT_SUFFIX) && (builder.font != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.font = null;
                        charsToSkip = 3;
                        continue;
                    }
                }

                //Handle HEX Coloring
                if (c == PERCENTAGE_CHAR) {
                    if (StringUtils.startsWith(subLine, FORMATTING_CODE_HEX_COLOR_PREFIX) && !StringUtils.startsWith(subLine, FORMATTING_CODE_HEX_COLOR_SUFFIX) && (builder.textColor == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                        String s = (subLine.length() >= 11) ? StringUtils.substring(subLine, 1, 11) : EMPTY_STRING;
                        if (!StringUtils.endsWith(s, PERCENTAGE)) {
                            s = (subLine.length() >= 9) ? StringUtils.substring(subLine, 1, 9) : EMPTY_STRING;
                        }
                        if (StringUtils.endsWith(s, PERCENTAGE) && StringUtils.contains(subText, FORMATTING_CODE_HEX_COLOR_SUFFIX)) {
                            DrawableColor color = DrawableColor.of(StringUtils.substring(s, 0, s.length()-1));
                            if (color != DrawableColor.EMPTY) {
                                if (isStartOfLine || !builder.text.isEmpty()) {
                                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                }
                                builder.textColor = color;
                                charsToSkip = s.length();
                                continue;
                            }
                        }
                    }
                    if (StringUtils.startsWith(subText, FORMATTING_CODE_HEX_COLOR_SUFFIX) && (builder.textColor != null)) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.textColor = null;
                        charsToSkip = 2;
                        continue;
                    }
                }

                //Handle Bold
                if ((c == ASTERISK_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.bold && (builder.codeBlockContext == null) && !builder.plainText) {
                        String s2 = StringUtils.substring(markdownText, Math.min(markdownText.length(), index+2));
                        if (StringUtils.startsWith(subText, FORMATTING_CODE_BOLD_PREFIX_SUFFIX) && StringUtils.contains(s2, FORMATTING_CODE_BOLD_PREFIX_SUFFIX)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.bold = true;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                    if (builder.bold) {
                        if (StringUtils.startsWith(subText, FORMATTING_CODE_BOLD_PREFIX_SUFFIX)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.bold = false;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                }

                int indexPlusOne = Math.min(markdownText.length(), index + 1);

                //Handle Italic Underscore
                if ((c == UNDERSCORE_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.italic && (builder.codeBlockContext == null) && !builder.plainText) {
                        String s = StringUtils.substring(markdownText, indexPlusOne);
                        if (StringUtils.contains(s, UNDERSCORE_CHAR)) {
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
                        String s2 = StringUtils.substring(markdownText, indexPlusOne);
                        if (!StringUtils.startsWith(subText, FORMATTING_CODE_BOLD_PREFIX_SUFFIX) && StringUtils.contains(s2, ASTERISK_CHAR)) {
                            boolean isEndSingleAsterisk = false;
                            int index2 = 0;
                            for (char c2 : s2.toCharArray()) {
                                if (c2 == ASTERISK_CHAR) {
                                    String s3 = StringUtils.substring(s2, index2);
                                    if (!StringUtils.startsWith(s3, FORMATTING_CODE_BOLD_PREFIX_SUFFIX)) {
                                        isEndSingleAsterisk = true;
                                        break;
                                    }
                                }
                                index2++;
                            }
                            if (isEndSingleAsterisk) {
                                if (isStartOfLine || !builder.text.isEmpty()) {
                                    lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                                }
                                builder.italic = true;
                                continue;
                            }
                        }
                    }
                    if (builder.italic && !italicUnderscore) {
                        if (!StringUtils.startsWith(subText, FORMATTING_CODE_BOLD_PREFIX_SUFFIX)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.italic = false;
                            continue;
                        }
                    }
                }

                //Handle Strikethrough
                if ((c == TILDE_CHAR) && !isCharacterEscaped(markdownText, index)) {
                    if (!builder.strikethrough && (builder.codeBlockContext == null) && !builder.plainText) {
                        String s = StringUtils.substring(markdownText, indexPlusOne);
                        if (StringUtils.contains(s, TILDE_CHAR)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.strikethrough = true;
                            continue;
                        }
                    }
                    if (builder.strikethrough) {
                        lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                        builder.strikethrough = false;
                        continue;
                    }
                }

                //Handle Hyperlink Image
                if (isStartOfLine && (c == OPEN_SQUARE_BRACKETS_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (StringUtils.startsWith(currentLine, FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX)) {
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
                if (isStartOfLine && (c == EXCLAMATION_MARK_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (StringUtils.startsWith(currentLine, FORMATTING_CODE_IMAGE_PREFIX)) {
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
                if ((c == OPEN_SQUARE_BRACKETS_CHAR) && (builder.hyperlink == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                    String s2 = StringUtils.substring(markdownText, indexPlusOne);
                    if (StringUtils.contains(s2, FORMATTING_CODE_HYPERLINK_INNER_PREFIX) && StringUtils.contains(s2, CLOSE_ROUND_BRACKETS_CHAR)) {
                        String hyperlink = getHyperlinkFromLine(subText);
                        if (hyperlink != null) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                // Check if current text ends with space to set endOfWord properly
                                boolean endsWithSpace = builder.text.toString().endsWith(" ");
                                lastBuiltFragment = addFragment(fragments, builder.build(false, endsWithSpace));
                            }
                            builder.hyperlink = new Hyperlink();
                            builder.hyperlink.link = hyperlink;
                            continue;
                        }
                    }
                }
                if ((c == CLOSE_SQUARE_BRACKETS_CHAR) && (builder.hyperlink != null)) {
                    if (StringUtils.startsWith(subText, FORMATTING_CODE_HYPERLINK_INNER_PREFIX)) {
                        // Check if there's whitespace after the closing parenthesis to determine endOfWord
                        int skipLength = 2 + builder.hyperlink.link.length();
                        boolean hasSpaceAfter = false;
                        if (index + skipLength < markdownText.length()) {
                            char afterChar = markdownText.charAt(index + skipLength);
                            hasSpaceAfter = (afterChar == SPACE_CHAR) || (afterChar == NEWLINE_CHAR);
                        }
                        lastBuiltFragment = addFragment(fragments, builder.build(false, hasSpaceAfter));
                        charsToSkip = skipLength;
                        builder.hyperlink = null;
                        continue;
                    }
                }

                //Handle Quote
                if (isStartOfLine && (c == GREATER_THAN_CHAR) && (builder.quoteContext == null) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (StringUtils.startsWith(subText, FORMATTING_CODE_QUOTE_PREFIX)) {
                        builder.quoteContext = new QuoteContext();
                        charsToSkip = 1;
                        continue;
                    }
                }
                if (isStartOfLine && (builder.quoteContext != null)) {
                    if (StringUtils.trim(currentLine).isEmpty()) {
                        builder.quoteContext = null; //it's important to disable quote BEFORE building the fragment
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        queueNewLine = true;
                        continue;
                    }
                }

                // Handle Bullet Lists (all levels)
                if (isStartOfLine && (builder.codeBlockContext == null) && !builder.plainText) {
                    // Match any bullet list pattern (supports 2 spaces per level)
                    Matcher bulletMatcher = Pattern.compile(BULLET_LIST_PATTERN).matcher(currentLine);
                    if (bulletMatcher.find()) {
                        int indentSpaces = bulletMatcher.group(1).length();
                        int level = (indentSpaces / 2) + 1;

                        if ((level >= 1) && (level <= 10)) {
                            // Calculate characters to skip (indent + "- "), subtract one because the current char is already consumed
                            charsToSkip = indentSpaces + 1;

                            // Configure builder
                            builder.bulletListLevel = level;
                            builder.bulletListItemStart = true;

                            continue;
                        }
                    }
                }

                //Handle Separation Line
                if (isStartOfLine && (c == MINUS_CHAR) && (builder.codeBlockContext == null) && !builder.plainText) {
                    if (StringUtils.startsWith(currentLine, FORMATTING_CODE_SEPARATION_LINE_PREFIX) && MINUS_CHARACTER_FILTER.isAllowedText(StringUtils.replace(currentLine, SPACE, EMPTY_STRING))) {
                        builder.separationLine = true;
                        builder.text = new StringBuilder(FORMATTING_CODE_SEPARATION_LINE_PREFIX);
                        lastBuiltFragment = addFragment(fragments, builder.build(true, true));
                        skipLine = true;
                        continue;
                    }
                }

                //Handle Code Block Single Line
                if ((c == GRAVE_ACCENT_CHAR) && !builder.plainText) {
                    if (builder.codeBlockContext == null) {
                        if (!StringUtils.startsWith(subLine, FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX) && isFormattedBlock(subText, GRAVE_ACCENT_CHAR, true)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.codeBlockContext = new CodeBlockContext();
                            builder.codeBlockContext.singleLine = true;
                            continue;
                        }
                    }
                    if ((builder.codeBlockContext != null) && builder.codeBlockContext.singleLine) {
                        if (!StringUtils.startsWith(subLine, FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.codeBlockContext = null;
                            continue;
                        }
                    }
                }

                //Handle Code Block Multi Line
                if (isStartOfLine && (c == GRAVE_ACCENT_CHAR) && (currentLine.length() == 3) && !builder.plainText) {
                    if (builder.codeBlockContext == null) {
                        if (StringUtils.startsWith(currentLine, FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX) && isFormattedBlock(subText, GRAVE_ACCENT_CHAR, false)) {
                            builder.codeBlockContext = new CodeBlockContext();
                            builder.codeBlockContext.singleLine = false;
                            skipLine = true;
                            continue;
                        }
                    }
                    if ((builder.codeBlockContext != null) && !builder.codeBlockContext.singleLine) {
                        if (StringUtils.startsWith(currentLine, FORMATTING_CODE_CODE_BLOCK_MULTI_LINE_PREFIX_SUFFIX)) {
                            builder.codeBlockContext = null;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Plain Text Single Line
                if ((c == SEMICOLON_CHAR) && !StringUtils.startsWith(subLine, FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX)) {
                    if (!builder.plainText) {
                        String s2 = StringUtils.substring(markdownText, Math.min(markdownText.length(), index+2));
                        if (StringUtils.startsWith(subText, FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX) && StringUtils.contains(s2, FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX)) {
                            if (isStartOfLine || !builder.text.isEmpty()) {
                                lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            }
                            builder.plainText = true;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                    if (builder.plainText) {
                        if (StringUtils.startsWith(subText, FORMATTING_CODE_PLAIN_TEXT_SINGLE_LINE_PREFIX_SUFFIX)) {
                            lastBuiltFragment = addFragment(fragments, builder.build(false, false));
                            builder.plainText = false;
                            charsToSkip = 1;
                            continue;
                        }
                    }
                }

                //Handle Plain Text Multi Line
                if (isStartOfLine && (c == SEMICOLON_CHAR) && (currentLine.length() == 3)) {
                    if (StringUtils.startsWith(subLine, FORMATTING_CODE_PLAIN_TEXT_MULTI_LINE_PREFIX_SUFFIX)) {
                        if (!builder.plainText && isFormattedBlock(subText, SEMICOLON_CHAR, false)) {
                            builder.plainText = true;
                            skipLine = true;
                            continue;
                        }
                        if (builder.plainText && removeFromString(subLine, SEMICOLON, SPACE, NEWLINE).isEmpty()) {
                            builder.plainText = false;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Alignment : Centered
                if (isStartOfLine && (c == CIRCUMFLEX_CHAR) && (currentLine.length() == 3)) {
                    if (StringUtils.startsWith(subLine, FORMATTING_CODE_ALIGNMENT_CENTERED_PREFIX_SUFFIX) && (builder.codeBlockContext == null)) {
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, CIRCUMFLEX_CHAR, false)) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.CENTERED;
                            skipLine = true;
                            continue;
                        }
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) && removeFromString(subLine, CIRCUMFLEX, SPACE, NEWLINE).isEmpty()) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
                            skipLine = true;
                            continue;
                        }
                    }
                }

                //Handle Alignment : Right
                if (isStartOfLine && (c == VERTICAL_BAR_CHAR) && (currentLine.length() == 3)) {
                    if (StringUtils.startsWith(subLine, FORMATTING_CODE_ALIGNMENT_RIGHT_PREFIX_SUFFIX) && (builder.codeBlockContext == null)) {
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) && isFormattedBlock(subText, VERTICAL_BAR_CHAR, false)) {
                            builder.alignment = MarkdownRenderer.MarkdownLineAlignment.RIGHT;
                            skipLine = true;
                            continue;
                        }
                        if ((builder.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) && removeFromString(subLine, VERTICAL_BAR, SPACE, NEWLINE).isEmpty()) {
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
                if (!isStartOfLine && (lastBuiltFragment != null) && builder.text.toString().equals(SPACE)) {
                    lastBuiltFragment.endOfWord = true;
                    lastBuiltFragment.text += SPACE;
                    lastBuiltFragment.updateWidth();
                    builder.clearText();
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
                queueNewLine = true;
            }

        }

        //Manually build the last fragment of the last line, because it doesn't end with "\n"
        fragments.add(builder.build(true, true));

        return fragments;

    }

    protected static String removeFromString(@NotNull String in, String... remove) {
        for (String s : remove) {
            in = StringUtils.replace(in, s, EMPTY_STRING);
        }
        return in;
    }

    protected static MarkdownTextFragment addFragment(List<MarkdownTextFragment> fragments, MarkdownTextFragment fragment) {
        fragments.add(fragment);
        return fragment;
    }

    @NotNull
    protected static String unescapeMarkdownText(@NotNull String text) {
        if (!StringUtils.contains(text, BACKSLASH_CHAR)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if ((c == BACKSLASH_CHAR) && (i + 1 < length)) {
                char next = text.charAt(i + 1);
                if (isEscapableMarkdownCharacter(next) && !isCharacterEscaped(text, i)) {
                    result.append(next);
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
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

    protected static void setImageToBuilder(@NotNull FragmentBuilder builder, @NotNull String imageSource) {
        builder.imageSupplier = ResourceSupplier.image(imageSource);
    }

    @NotNull
    protected static String getLine(@NotNull String text) {
        try {
            if (!StringUtils.contains(text, NEWLINE)) return text;
            if (StringUtils.startsWith(text, NEWLINE)) return EMPTY_STRING;
            return StringUtils.split(text, NEWLINE, 2)[0];
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get line of Markdown text!", ex);
        }
        return text;
    }

    protected static boolean isFormattedBlock(String text, char formatChar, boolean singleLine) {
        String longFormatCode = EMPTY_STRING + formatChar + formatChar + formatChar;
        if (singleLine) {
            if (StringUtils.startsWith(text, EMPTY_STRING + formatChar) && !StringUtils.startsWith(text, longFormatCode)) {
                int i = -1;
                boolean endFound = false;
                for (char c : text.toCharArray()) {
                    i++;
                    if (i == 0) {
                        continue;
                    }
                    if (c == NEWLINE_CHAR) {
                        break;
                    }
                    if (c == formatChar) {
                        if (!StringUtils.startsWith(StringUtils.substring(text, i), EMPTY_STRING + formatChar + formatChar + formatChar)) {
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
                    if (c == NEWLINE_CHAR) {
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
        if (StringUtils.startsWith(line, FORMATTING_CODE_HYPERLINK_IMAGE_PREFIX) && StringUtils.contains(line, FORMATTING_CODE_HYPERLINK_INNER_PREFIX) && StringUtils.contains(line, CLOSE_ROUND_BRACKETS_CHAR)) {
            String imageLink = null;
            String hyperLink = null;
            int index = -1;
            for (char ignored : line.toCharArray()) {
                index++;
                String sub = StringUtils.substring(line, index);
                if (index < 1) {
                    continue;
                }
                if (StringUtils.startsWith(sub, FORMATTING_CODE_IMAGE_PREFIX)) {
                    imageLink = getImageFromLine(sub);
                    if (imageLink != null) {
                        String s = StringUtils.split(sub, CLOSE_ROUND_BRACKETS, 2)[0] + CLOSE_ROUND_BRACKETS;
                        hyperLink = getHyperlinkFromLine(line.replace(s, EMPTY_STRING));
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
        if (StringUtils.startsWith(line, FORMATTING_CODE_IMAGE_PREFIX) && StringUtils.contains(line, FORMATTING_CODE_HYPERLINK_INNER_PREFIX) && StringUtils.contains(line, CLOSE_ROUND_BRACKETS)) {
            boolean beforeClosedBrackets = true;
            boolean isInsideImageLink = false;
            StringBuilder imageLink = new StringBuilder();
            boolean openRoundBracketsFound = false;
            String s = StringUtils.substring(line, 2);
            int index = -1;
            for (char c : s.toCharArray()) {
                index++;
                if (c == NEWLINE_CHAR) {
                    return null;
                }
                if (c == CLOSE_SQUARE_BRACKETS_CHAR) {
                    if (!beforeClosedBrackets) {
                        return null;
                    }
                    beforeClosedBrackets = false;
                    if (StringUtils.startsWith(StringUtils.substring(s, index), FORMATTING_CODE_HYPERLINK_INNER_PREFIX)) {
                        isInsideImageLink = true;
                        continue;
                    }
                }
                if ((c == OPEN_SQUARE_BRACKETS_CHAR) && beforeClosedBrackets) {
                    return null;
                }
                if (isInsideImageLink) {
                    if (c == OPEN_ROUND_BRACKETS_CHAR) {
                        if (!openRoundBracketsFound) {
                            openRoundBracketsFound = true;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    if (c == CLOSE_ROUND_BRACKETS_CHAR) {
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
        if (StringUtils.startsWith(line, OPEN_SQUARE_BRACKETS) && StringUtils.contains(line, FORMATTING_CODE_HYPERLINK_INNER_PREFIX) && StringUtils.contains(line, CLOSE_ROUND_BRACKETS)) {
            boolean beforeClosedBrackets = true;
            boolean isInsideHyperlink = false;
            StringBuilder hyperlink = new StringBuilder();
            boolean openRoundBracketsFound = false;
            String s = StringUtils.substring(line, 1);
            int index = -1;
            for (char c : s.toCharArray()) {
                index++;
                if (c == NEWLINE_CHAR) {
                    return null;
                }
                if (c == CLOSE_SQUARE_BRACKETS_CHAR) {
                    if (!beforeClosedBrackets) {
                        return null;
                    }
                    beforeClosedBrackets = false;
                    if (StringUtils.startsWith(StringUtils.substring(s, index), FORMATTING_CODE_HYPERLINK_INNER_PREFIX)) {
                        isInsideHyperlink = true;
                        continue;
                    }
                }
                if ((c == OPEN_SQUARE_BRACKETS_CHAR) && beforeClosedBrackets) {
                    return null;
                }
                if (isInsideHyperlink) {
                    if (c == OPEN_ROUND_BRACKETS_CHAR) {
                        if (!openRoundBracketsFound) {
                            openRoundBracketsFound = true;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    if (c == CLOSE_ROUND_BRACKETS_CHAR) {
                        return hyperlink.toString();
                    }
                    hyperlink.append(c);
                }
            }
        }
        return null;
    }

    protected static boolean isTableSeparatorLine(@NotNull String line) {
        if (!containsUnescapedPipe(line)) {
            return false;
        }
        return TABLE_SEPARATOR_PATTERN.matcher(line).matches();
    }
    
    protected static boolean isTableRow(@NotNull String line) {
        return containsUnescapedPipe(line) && !isTableSeparatorLine(line);
    }
    
    @NotNull
    protected static List<String> parseTableRow(@NotNull String line) {
        List<String> cells = new ArrayList<>();
        String trimmed = line.trim();
        if (trimmed.startsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(VERTICAL_BAR)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> parts = splitByUnescapedPipes(trimmed);
        parts.forEach(part -> cells.add(part.trim()));
        return cells;
    }
    
    @NotNull
    protected static List<MarkdownTextFragment.TableCell.TableCellAlignment> parseTableAlignments(@NotNull String separatorLine) {
        List<MarkdownTextFragment.TableCell.TableCellAlignment> alignments = new ArrayList<>();
        String trimmed = separatorLine.trim();
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
    protected static String preprocessTables(@NotNull MarkdownRenderer renderer, @NotNull String markdownText, @NotNull List<MarkdownTextFragment.TableContext> tableContexts) {
        String[] lines = markdownText.split(NEWLINE);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i];
            String nextLine = (i + 1 < lines.length) ? lines[i + 1] : null;
            
            // Check if this is the start of a table
            if (isTableRow(currentLine) && nextLine != null && isTableSeparatorLine(nextLine)) {
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
                    List<MarkdownTextFragment> cellFragments = parse(renderer, headerCells.get(j), true);
                    for (MarkdownTextFragment frag : cellFragments) {
                        frag.tableContext = tableContext;
                    }
                    cell.fragments.addAll(cellFragments);
                    headerRow.cells.add(cell);
                }
                tableContext.rows.add(headerRow);
                
                // Skip the separator line
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
                        List<MarkdownTextFragment> cellFragments = parse(renderer, dataCells.get(j), true);
                        for (MarkdownTextFragment frag : cellFragments) {
                            frag.tableContext = tableContext;
                        }
                        cell.fragments.addAll(cellFragments);
                        dataRow.cells.add(cell);
                    }
                    tableContext.rows.add(dataRow);
                    i++;
                }
                
                // Store the table context
                tableContexts.add(tableContext);
                
                // Add a placeholder line in the result
                result.append("[TABLE_").append(tableContexts.size() - 1).append("]").append(NEWLINE);
                i--; // Adjust because the loop will increment
            } else {
                result.append(currentLine);
                if (i < lines.length - 1) {
                    result.append(NEWLINE);
                }
            }
        }
        
        return result.toString();
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

    protected static class FragmentBuilder {

        protected final MarkdownRenderer renderer;
        protected final boolean parseMarkdownEnabled;
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
        protected Identifier font = null;
        protected boolean plainText = false;
        protected MarkdownTextFragment.TableContext tableContext = null;

        protected FragmentBuilder(MarkdownRenderer renderer, boolean parseMarkdownEnabled) {
            this.renderer = renderer;
            this.parseMarkdownEnabled = parseMarkdownEnabled;
        }

        @NotNull
        protected MarkdownTextFragment build(boolean naturalLineBreakAfter, boolean endOfWord) {
            String fragmentText = this.text.toString();
            if (this.parseMarkdownEnabled && !this.plainText && (this.codeBlockContext == null)) {
                fragmentText = unescapeMarkdownText(fragmentText);
            }
            MarkdownTextFragment frag = new MarkdownTextFragment(this.renderer, fragmentText);
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
            this.text = new StringBuilder();
            return this;
        }

    }

}
