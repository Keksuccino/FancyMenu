//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class HighlightBracketsFormattingRule extends TextEditorFormattingRule {

    protected static final Style STYLE = Style.EMPTY.withColor(new Color(252, 223, 3).getRGB());

//    protected boolean cursorIsAtOpenBracket = false;
//    protected int depth = 0;

    @Override
    public void resetRule() {
//        this.cursorIsAtOpenBracket = false;
//        this.depth = 0;
    }

    @Override
    public @Nullable Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor) {
        String s = String.valueOf(atCharacterInLine);
        if (s.equals("{")) {
            TextEditorLine focusedLine = editor.getFocusedLine();
            if (focusedLine != null) {

            }
        }
        return null;
    }

    //TODO funktioniert so nicht
    private static int findCharacterIndexOfCounterBracket(TextEditorScreen editor, int startIndex, String s) {
        String fullText = editor.getText();
        if (fullText.length() >= 2) {
            if (s.equals("{")) {
                String afterBracket = fullText.substring(startIndex+1);
                int index = startIndex+1;
                int depth = 1;
                for (char c : afterBracket.toCharArray()) {
                    String s2 = String.valueOf(c);
                    if (s2.equals("{")) {
                        depth++;
                    }
                    if (s2.equals("}")) {
                        depth--;
                        if (depth == 0) {
                            return index;
                        }
                    }
                    index++;
                }
            }
            if (s.equals("}")) {
                String beforeBracket = fullText.substring(0, startIndex);
//                int index = startIndex+1;
//                int depth = 1;
//                for (char c : afterBracket.toCharArray()) {
//                    String s2 = String.valueOf(c);
//                    if (s2.equals("{")) {
//                        depth++;
//                    }
//                    if (s2.equals("}")) {
//                        depth--;
//                        if (depth == 0) {
//                            return index;
//                        }
//                    }
//                    index++;
//                }
            }
        }
        return -1;
    }

//    @Override
//    public @Nullable Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, TextEditorScreen editor) {
//        String s = String.valueOf(atCharacterInLine);
//        if (!this.cursorIsAtOpenBracket) {
//            this.depth = 0;
//        }
//        if (s.equals("{")) {
//            if (cursorPosInLine == atCharacterIndexInLine) {
//                this.cursorIsAtOpenBracket = true;
//                return STYLE;
//            }
//            this.depth++;
//        }
//        if (s.equals("}")) {
//            this.depth--;
//            if (this.cursorIsAtOpenBracket && (this.depth == 0)) {
//                this.cursorIsAtOpenBracket = false;
//                return STYLE;
//            }
//        }
//        return null;
//    }


}
