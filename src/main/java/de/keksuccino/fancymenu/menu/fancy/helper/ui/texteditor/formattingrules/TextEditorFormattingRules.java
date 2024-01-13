
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets.HighlightAngleBracketsFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets.HighlightCurlyBracketsFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets.HighlightRoundBracketsFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules.brackets.HighlightSquareBracketsFormattingRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TextEditorFormattingRules {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Class<? extends TextEditorFormattingRule>> RULE_CLASSES = new ArrayList<>();

    static {

        addRuleAtTop(HighlightPlaceholdersFormattingRule.class);

        addRuleAtBottom(HighlightAngleBracketsFormattingRule.class);
        addRuleAtBottom(HighlightCurlyBracketsFormattingRule.class);
        addRuleAtBottom(HighlightRoundBracketsFormattingRule.class);
        addRuleAtBottom(HighlightSquareBracketsFormattingRule.class);

    }

    public static void addRuleAtTop(Class<? extends TextEditorFormattingRule> rule) {
        if (!RULE_CLASSES.contains(rule)) {
            RULE_CLASSES.add(0, rule);
        }
    }

    public static void addRuleAtBottom(Class<? extends TextEditorFormattingRule> rule) {
        if (!RULE_CLASSES.contains(rule)) {
            RULE_CLASSES.add(rule);
        }
    }

    public static List<TextEditorFormattingRule> getRules() {
        List<TextEditorFormattingRule> r = new ArrayList<>();
        for (Class<? extends TextEditorFormattingRule> rule : RULE_CLASSES) {
            try {
                r.add(rule.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Unable to construct new instance of rule (" + ((rule != null) ? rule.getName() : "NULL") + ")!");
                LOGGER.error("[FANCYMENU] Rules need an empty public constructor!");
                e.printStackTrace();
            }
        }
        return r;
    }

}
