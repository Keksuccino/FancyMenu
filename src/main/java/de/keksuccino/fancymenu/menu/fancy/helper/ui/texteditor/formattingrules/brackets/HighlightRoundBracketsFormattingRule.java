//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets;

import net.minecraft.util.text.Color;
import net.minecraft.util.text.Style;

public class HighlightRoundBracketsFormattingRule extends HighlightBracketsFormattingRuleBase {

    protected static final Style STYLE = Style.EMPTY.withColor(Color.fromRgb(new java.awt.Color(252, 223, 3).getRGB()));

    @Override
    protected String getOpenBracketChar() {
        return "(";
    }

    @Override
    protected String getCloseBracketChar() {
        return ")";
    }

    @Override
    protected Style getHighlightStyle() {
        return STYLE;
    }

}
