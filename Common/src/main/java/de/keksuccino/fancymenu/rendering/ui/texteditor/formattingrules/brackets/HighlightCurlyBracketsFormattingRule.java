
package de.keksuccino.fancymenu.rendering.ui.texteditor.formattingrules.brackets;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
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
        return Style.EMPTY.withColor(UIBase.getUIColorScheme().textFormattingBracketsColor.getColorInt());
    }

}
