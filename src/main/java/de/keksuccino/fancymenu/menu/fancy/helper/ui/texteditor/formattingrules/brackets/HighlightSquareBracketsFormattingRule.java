//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets;

import net.minecraft.network.chat.Style;

import java.awt.*;

public class HighlightSquareBracketsFormattingRule extends HighlightBracketsFormattingRuleBase {

    protected static final Style STYLE = Style.EMPTY.withColor(new Color(252, 223, 3).getRGB());

    @Override
    protected String getOpenBracketChar() {
        return "[";
    }

    @Override
    protected String getCloseBracketChar() {
        return "]";
    }

    @Override
    protected Style getHighlightStyle() {
        return STYLE;
    }

}
