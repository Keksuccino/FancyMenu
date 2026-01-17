package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.formattingrules;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HighlightPlaceholdersFormattingRule extends TextEditorFormattingRule {

    // JSON syntax highlighting styles
    private final Style braceStyle = Style.EMPTY.withColor(UIBase.getUITheme().text_editor_text_formatting_nested_text_color_4.getColorInt());
    private final Style keyStyle = Style.EMPTY.withColor(UIBase.getUITheme().text_editor_text_formatting_nested_text_color_1.getColorInt());
    private final Style stringStyle = Style.EMPTY.withColor(UIBase.getUITheme().text_editor_text_formatting_nested_text_color_2.getColorInt());
    private final Style punctuationStyle = Style.EMPTY.withColor(UIBase.getUITheme().text_editor_text_formatting_nested_text_color_5.getColorInt());

    // Store character positions with their styles
    private Map<TextEditorLine, SortedMap<Integer, Style>> characterStyles = new HashMap<>();
    // Track lines for position calculations
    private final List<TextEditorLine> allLines = new ArrayList<>();
    // Cache the complete text
    private String fullText;

    @Override
    public void resetRule(TextEditorScreen editor) {
        characterStyles.clear();
        allLines.clear();
        allLines.addAll(editor.getLines());

        // Build full text from all lines
        StringBuilder fullTextBuilder = new StringBuilder();
        Map<Integer, TextEditorLine> positionToLine = new HashMap<>();
        Map<TextEditorLine, Integer> lineStartPositions = new HashMap<>();

        int globalPos = 0;
        for (TextEditorLine line : allLines) {
            lineStartPositions.put(line, globalPos);
            String lineContent = line.getValue();
            for (int i = 0; i < lineContent.length(); i++) {
                positionToLine.put(globalPos, line);
                globalPos++;
            }
            fullTextBuilder.append(lineContent).append('\n');
            globalPos++;
        }

        fullText = fullTextBuilder.toString();

        // Process all JSON objects in the text
        List<Integer> openBracePositions = findAllOpenBraces(fullText);
        for (int openBracePos : openBracePositions) {
            // Try to process as a potential placeholder
            processJsonObject(fullText, openBracePos, positionToLine, lineStartPositions);
        }
    }

    private List<Integer> findAllOpenBraces(String text) {
        List<Integer> positions = new ArrayList<>();
        int currentPos = 0;

        while (currentPos < text.length()) {
            int openBracePos = text.indexOf('{', currentPos);
            if (openBracePos == -1) break;

            positions.add(openBracePos);
            currentPos = openBracePos + 1;
        }

        return positions;
    }

    private void processJsonObject(String text, int openBracePos, Map<Integer, TextEditorLine> posToLine, Map<TextEditorLine, Integer> lineStarts) {
        int closeBracePos = findMatchingCloseBrace(text, openBracePos);
        if (closeBracePos == -1) return;

        // Check if this might be a placeholder
        String jsonContent = text.substring(openBracePos, closeBracePos + 1);
        if (!isPlaceholderJson(jsonContent)) return;

        // Apply styles to the entire placeholder
        tokenizeJson(text, openBracePos, closeBracePos, posToLine, lineStarts);
    }

    private boolean isPlaceholderJson(String json) {
        // Simple check for placeholder format
        // Only require "placeholder" field since "values" is optional
        return json.contains("\"placeholder\"");
    }

    private int findMatchingCloseBrace(String text, int openBracePos) {
        int depth = 1;
        int pos = openBracePos + 1;
        boolean inString = false;
        boolean escaped = false;

        while (pos < text.length() && depth > 0) {
            char c = text.charAt(pos);

            if (inString) {
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    inString = false;
                } else {
                    escaped = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
            }

            if (depth == 0) {
                return pos;
            }

            pos++;
        }

        return -1; // No matching close brace found
    }

    private void tokenizeJson(String text, int startPos, int endPos, Map<Integer, TextEditorLine> posToLine, Map<TextEditorLine, Integer> lineStarts) {
        int pos = startPos;
        boolean inString = false;
        boolean escaped = false;
        boolean potentialKey = false;
        int stringStartPos = -1;

        while (pos <= endPos) {
            char c = text.charAt(pos);

            if (inString) {
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    // End of string
                    inString = false;

                    // Determine if this was a key or a value
                    int endStringPos = pos;

                    // Skip whitespace
                    int checkPos = endStringPos + 1;
                    while (checkPos < text.length() && Character.isWhitespace(text.charAt(checkPos))) {
                        checkPos++;
                    }

                    // If we find a colon, it was a key
                    boolean isKey = checkPos < text.length() && text.charAt(checkPos) == ':';
                    Style style = isKey ? keyStyle : stringStyle;

                    // Apply style to the opening and closing quotes
                    applyStyleToPosition(stringStartPos, posToLine, lineStarts, style);
                    applyStyleToPosition(endStringPos, posToLine, lineStarts, style);

                    // Apply style to the content between quotes
                    for (int i = stringStartPos + 1; i < endStringPos; i++) {
                        applyStyleToPosition(i, posToLine, lineStarts, style);
                    }

                    // Check for placeholder nested in a string value
                    if (!isKey && endStringPos - stringStartPos > 3) {
                        String stringValue = text.substring(stringStartPos + 1, endStringPos);
                        if (stringValue.startsWith("{") && stringValue.endsWith("}") && stringValue.contains("\"placeholder\"")) {
                            // Process nested placeholder with adjusted offsets
                            int finalStringStartPos = stringStartPos;
                            int finalStringStartPos1 = stringStartPos;
                            tokenizeJson(
                                    stringValue,
                                    0,
                                    stringValue.length() - 1,
                                    (i) -> posToLine.get(finalStringStartPos + 1 + i),
                                    (line) -> lineStarts.get(line) + finalStringStartPos1 + 1
                            );
                        }
                    }
                } else {
                    escaped = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    stringStartPos = pos;
                } else if (c == '{' || c == '}') {
                    applyStyleToPosition(pos, posToLine, lineStarts, braceStyle);
                } else if (c == ':' || c == ',') {
                    applyStyleToPosition(pos, posToLine, lineStarts, punctuationStyle);
                }
            }

            pos++;
        }
    }

    // Function overloads to handle both maps and functional interfaces
    private void applyStyleToPosition(int pos, Map<Integer, TextEditorLine> posToLine, Map<TextEditorLine, Integer> lineStarts, Style style) {
        TextEditorLine line = posToLine.get(pos);
        if (line != null) {
            int lineStartPos = lineStarts.get(line);
            int posInLine = pos - lineStartPos;

            if (posInLine >= 0 && posInLine < line.getValue().length()) {
                characterStyles.computeIfAbsent(line, k -> new TreeMap<>()).put(posInLine, style);
            }
        }
    }

    private void tokenizeJson(String text, int startPos, int endPos, PosToLineMapper posToLine, LineToStartPosMapper lineStarts) {
        int pos = startPos;
        boolean inString = false;
        boolean escaped = false;
        int stringStartPos = -1;

        while (pos <= endPos) {
            char c = text.charAt(pos);

            if (inString) {
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    // End of string
                    inString = false;

                    // Determine if this was a key or a value
                    int endStringPos = pos;

                    // Skip whitespace
                    int checkPos = endStringPos + 1;
                    while (checkPos < text.length() && Character.isWhitespace(text.charAt(checkPos))) {
                        checkPos++;
                    }

                    // If we find a colon, it was a key
                    boolean isKey = checkPos < text.length() && text.charAt(checkPos) == ':';
                    Style style = isKey ? keyStyle : stringStyle;

                    // Apply style to the opening and closing quotes
                    applyStyleToPosition(stringStartPos, posToLine, lineStarts, style);
                    applyStyleToPosition(endStringPos, posToLine, lineStarts, style);

                    // Apply style to the content between quotes
                    for (int i = stringStartPos + 1; i < endStringPos; i++) {
                        applyStyleToPosition(i, posToLine, lineStarts, style);
                    }
                } else {
                    escaped = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    stringStartPos = pos;
                } else if (c == '{' || c == '}') {
                    applyStyleToPosition(pos, posToLine, lineStarts, braceStyle);
                } else if (c == ':' || c == ',') {
                    applyStyleToPosition(pos, posToLine, lineStarts, punctuationStyle);
                }
            }

            pos++;
        }
    }

    private void applyStyleToPosition(int pos, PosToLineMapper posToLine, LineToStartPosMapper lineStarts, Style style) {
        TextEditorLine line = posToLine.apply(pos);
        if (line != null) {
            int lineStartPos = lineStarts.apply(line);
            int posInLine = pos - lineStartPos;

            if (posInLine >= 0 && posInLine < line.getValue().length()) {
                characterStyles.computeIfAbsent(line, k -> new TreeMap<>()).put(posInLine, style);
            }
        }
    }

    @Override
    public @Nullable Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine,
                                    TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor) {
        SortedMap<Integer, Style> lineStyles = characterStyles.get(inLine);
        if (lineStyles != null && lineStyles.containsKey(atCharacterIndexInLine)) {
            return lineStyles.get(atCharacterIndexInLine);
        }
        return null;
    }

    // Functional interface versions for nested placeholders
    @FunctionalInterface
    private interface PosToLineMapper {
        TextEditorLine apply(int pos);
    }

    @FunctionalInterface
    private interface LineToStartPosMapper {
        int apply(TextEditorLine line);
    }

}