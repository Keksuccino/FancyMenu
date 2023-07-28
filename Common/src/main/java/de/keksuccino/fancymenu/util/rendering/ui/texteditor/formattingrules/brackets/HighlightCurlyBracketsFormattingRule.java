
package de.keksuccino.fancymenu.util.rendering.ui.texteditor.formattingrules.brackets;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Style;

public class HighlightCurlyBracketsFormattingRule extends HighlightBracketsFormattingRuleBase {

    @Override
    protected String getOpenBracketChar() {
        return "{";
    }

    @Override
    protected String getCloseBracketChar() {
        return "}";
    }

    @Override
    protected Style getHighlightStyle() {
        return Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_brackets_color.getColorInt());
    }

}
