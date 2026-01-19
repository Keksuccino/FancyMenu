
package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public abstract class TextEditorFormattingRule {

    public abstract void resetRule(TextEditorWindowBody editor);

    /**
     * - Use Style.EMPTY as base<br>
     * - Return NULL if no style should get applied to the character
     **/
    @Nullable
    public abstract Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorWindowBody editor);

}
