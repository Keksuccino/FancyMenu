//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HighlightBracketsFormattingRuleBase extends TextEditorFormattingRule {

    protected TextEditorLine openBracketLine = null;
    protected int openBracketInLineIndex = -1;
    protected TextEditorLine closeBracketLine = null;
    protected int closeBracketInLineIndex = -1;
    
    protected abstract String getOpenBracketChar();
    
    protected abstract String getCloseBracketChar();

    protected abstract Style getHighlightStyle();
    
    @Override
    public void resetRule(TextEditorScreen editor) {
        this.openBracketLine = null;
        this.openBracketInLineIndex = -1;
        this.closeBracketLine = null;
        this.closeBracketInLineIndex = -1;
        TextEditorLine focusedLine = editor.getFocusedLine();
        if ((focusedLine != null) && (focusedLine.getValue().length() > 0)) {
            String textBeforeCursor = focusedLine.getValue().substring(0, focusedLine.getCursorPosition());
            String textAfterCursor = focusedLine.getValue().substring(focusedLine.getCursorPosition());
            int focusedLineIndex = editor.getFocusedLineIndex();
            if (textAfterCursor.startsWith(this.getOpenBracketChar())) {
                this.openBracketLine = focusedLine;
                this.openBracketInLineIndex = textBeforeCursor.length();
                List<TextEditorLine> lines = new ArrayList<>();
                if (focusedLineIndex == editor.getLineCount()-1) {
                    lines.add(focusedLine);
                } else {
                    lines.addAll(editor.textFieldLines.subList(focusedLineIndex, editor.getLineCount()));
                }
                int depth = 1;
                for (TextEditorLine line : lines) {
                    if (line.getValue().contains(this.getOpenBracketChar()) || line.getValue().contains(this.getCloseBracketChar())) {
                        int inLineIndex = 0;
                        String lineValue = line.getValue();
                        if (line == focusedLine) {
                            lineValue = textAfterCursor.substring(1);
                            inLineIndex = textBeforeCursor.length()+1;
                        }
                        for (char c : lineValue.toCharArray()) {
                            String s = String.valueOf(c);
                            if (s.equals(this.getOpenBracketChar())) {
                                depth++;
                            }
                            if (s.equals(this.getCloseBracketChar())) {
                                depth--;
                                if (depth == 0) {
                                    this.closeBracketLine = line;
                                    this.closeBracketInLineIndex = inLineIndex;
                                    return;
                                }
                            }
                            inLineIndex++;
                        }
                    }
                }
            }
            if (textBeforeCursor.endsWith(this.getCloseBracketChar())) {
                this.closeBracketLine = focusedLine;
                this.closeBracketInLineIndex = textBeforeCursor.length()-1;
                List<TextEditorLine> lines = new ArrayList<>();
                if (focusedLineIndex == 0) {
                    lines.add(focusedLine);
                } else {
                    lines.addAll(editor.textFieldLines.subList(0, focusedLineIndex+1));
                }
                Collections.reverse(lines);
                int depth = 1;
                for (TextEditorLine line : lines) {
                    if (line.getValue().contains(this.getOpenBracketChar()) || line.getValue().contains(this.getCloseBracketChar())) {
                        int inLineIndex = line.getValue().length()-1;
                        String lineValue = new StringBuilder(line.getValue()).reverse().toString();
                        if (line == focusedLine) {
                            lineValue = new StringBuilder(textBeforeCursor).reverse().toString().substring(1);
                            inLineIndex = lineValue.length()-1;
                        }
                        for (char c : lineValue.toCharArray()) {
                            String s = String.valueOf(c);
                            if (s.equals(this.getCloseBracketChar())) {
                                depth++;
                            }
                            if (s.equals(this.getOpenBracketChar())) {
                                depth--;
                                if (depth == 0) {
                                    this.openBracketLine = line;
                                    this.openBracketInLineIndex = inLineIndex;
                                    return;
                                }
                            }
                            inLineIndex--;
                        }
                    }
                }
            }
        }
    }

    @Override
    public @Nullable Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor) {
        if ((this.openBracketLine != null) && (this.closeBracketLine != null)) {
            String s = String.valueOf(atCharacterInLine);
            if (s.equals(this.getOpenBracketChar()) && (inLine == this.openBracketLine) && (atCharacterIndexInLine == this.openBracketInLineIndex)) {
                return this.getHighlightStyle();
            }
            if (s.equals(this.getCloseBracketChar()) && (inLine == this.closeBracketLine) && (atCharacterIndexInLine == this.closeBracketInLineIndex)) {
                return this.getHighlightStyle();
            }
        }
        return null;
    }

}
