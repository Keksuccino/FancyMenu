//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.realtime;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class IsRealTimeMinuteVisibilityRequirement extends VisibilityRequirement {

    public IsRealTimeMinuteVisibilityRequirement() {
        super("fancymenu_visibility_requirement_is_realtime_minute");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(String value) {

        List<Integer> l = new ArrayList<>();
        if (value != null) {
            if (value.contains(",")) {
                for (String s : value.replace(" ", "").split("[,]")) {
                    if (MathUtils.isInteger(s)) {
                        l.add(Integer.parseInt(s));
                    }
                }
            } else {
                if (MathUtils.isInteger(value.replace(" ", ""))) {
                    l.add(Integer.parseInt(value.replace(" ", "")));
                }
            }
        }
        if (!l.isEmpty()) {
            Calendar c = Calendar.getInstance();
            if (c != null) {
                return l.contains(c.get(Calendar.MINUTE));
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.desc"), "%n%"));
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.valuename");
    }

    @Override
    public String getValuePreset() {
        return "20, 49";
    }

    @Override
    public CharacterFilter getValueInputFieldFilter() {
        CharacterFilter filter = CharacterFilter.getIntegerCharacterFiler();
        filter.addAllowedCharacters(",", " ");
        return filter;
    }

}
