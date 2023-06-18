package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.realtime;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class IsRealTimeWeekDayRequirement extends LoadingRequirement {

    public IsRealTimeWeekDayRequirement() {
        super("fancymenu_visibility_requirement_is_realtime_week_day");
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
                return l.contains(c.get(Calendar.DAY_OF_WEEK));
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.realtimeweekday");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.realtimeweekday.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.realtime");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.realtimeweekday.valuename");
    }

    @Override
    public String getValuePreset() {
        return "2";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
