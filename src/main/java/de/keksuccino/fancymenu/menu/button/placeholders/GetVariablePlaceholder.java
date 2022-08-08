//---
package de.keksuccino.fancymenu.menu.button.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;

import java.util.List;

public class GetVariablePlaceholder extends PlaceholderTextContainer {

    public GetVariablePlaceholder() {
        super("fancymenu_placeholder_get_variable");
    }

    @Override
    public String replacePlaceholders(String rawIn) {

        String s = rawIn;

        List<String> l = getPlaceholdersWithValue(s, "%get_variable:");
        for (String s2 : l) {
            if (s2.contains(":")) {
                String blank = getPlaceholderWithoutPercentPrefixSuffix(s2);
                String name = blank.split("[:]", 2)[1];
                String val = VariableHandler.getVariable(name);
                if (val != null) {
                    s = s.replace(s2, val);
                }
            }
        }

        return s;

    }

    @Override
    public String getPlaceholder() {
        return "%get_variable:<variable_name>%";
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.placeholder.get_variable");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.get_variable.desc"), "%n%");
    }

}
