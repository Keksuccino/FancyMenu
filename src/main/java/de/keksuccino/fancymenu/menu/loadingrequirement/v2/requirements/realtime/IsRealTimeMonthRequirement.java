package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.realtime;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class IsRealTimeMonthRequirement extends LoadingRequirement {

    public IsRealTimeMonthRequirement() {
        super("fancymenu_visibility_requirement_is_realtime_month");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

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
                return l.contains(c.get(Calendar.MONTH)+1);
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.realtime");
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth.valuename");
    }

    @Override
    public String getValuePreset() {
        return "5";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
