//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.TextStyle;

public class HighlightAngleBracketsFormattingRule extends HighlightBracketsFormattingRuleBase {

    protected static final TextStyle STYLE = new TextStyle().setColorRGB(new java.awt.Color(252, 223, 3).getRGB());

    @Override
    protected String getOpenBracketChar() {
        return "<";
    }

    @Override
    protected String getCloseBracketChar() {
        return ">";
    }

    @Override
    protected TextStyle getHighlightStyle() {
        return STYLE;
    }

}
