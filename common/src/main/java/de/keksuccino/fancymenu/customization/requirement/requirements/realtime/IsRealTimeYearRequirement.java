package de.keksuccino.fancymenu.customization.requirement.requirements.realtime;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsRealTimeYearRequirement extends Requirement {

    public IsRealTimeYearRequirement() {
        super("fancymenu_visibility_requirement_is_realtime_year");
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
                return l.contains(c.get(Calendar.YEAR));
            }
        }

        return false;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.realtime_year");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.realtime_year.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.realtime");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.realtime_year.value_name");
    }

    @Override
    public String getValuePreset() {
        return "2012";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
