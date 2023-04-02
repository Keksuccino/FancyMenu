//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.TextStyle;

public abstract class TextEditorFormattingRule {

    public abstract void resetRule(TextEditorScreen editor);

    /**
     * - Use TextStyle.EMPTY as base<br>
     * - Return NULL if no style should get applied to the character
     **/
    public abstract TextStyle getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor);

}
