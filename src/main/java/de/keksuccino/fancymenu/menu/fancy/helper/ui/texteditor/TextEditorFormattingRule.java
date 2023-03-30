//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor;

import net.minecraft.util.text.Style;

public abstract class TextEditorFormattingRule {

    public abstract void resetRule(TextEditorScreen editor);

    /**
     * - Use Style.EMPTY as base<br>
     * - Return NULL if no style should get applied to the character
     **/
    public abstract Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor);

}
