package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsGuiScaleRequirement extends Requirement {

    public IsGuiScaleRequirement() {
        super("fancymenu_loading_requirement_is_gui_scale");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value == null) return false;
        List<String> conditions = this.parseValues(value);
        if (conditions.isEmpty()) return false;
        return matchesGuiScaleConditions(conditions, Minecraft.getInstance().getWindow().getGuiScale());
    }

    /**
     * Equality tokens are alternatives, while relational tokens stay cumulative so legacy ranges such as
     * {@code >1.20,<3.0} keep their established behavior.
     */
    boolean matchesGuiScaleConditions(@Nullable String value, double windowScale) {
        if (value == null) return false;
        List<String> conditions = this.parseValues(value);
        if (conditions.isEmpty()) return false;
        return matchesGuiScaleConditions(conditions, windowScale);
    }

    private static boolean matchesGuiScaleConditions(@NotNull List<String> conditions, double windowScale) {
        boolean hasEqualityCondition = false;
        boolean equalityConditionMatched = false;
        for (String condition : conditions) {
            if (condition.startsWith("double:")) {
                hasEqualityCondition = true;
                if (checkForGuiScale(condition, windowScale)) equalityConditionMatched = true;
            } else if (!checkForGuiScale(condition, windowScale)) {
                return false;
            }
        }
        return !hasEqualityCondition || equalityConditionMatched;
    }

    protected List<String> parseValues(String value) {
        List<String> l1 = new ArrayList<>();
        if (value.contains(",")) {
            l1.addAll(Arrays.asList(value.replace(" ", "").split(",")));
        } else {
            if (value.length() > 0) {
                l1.add(value.replace(" ", ""));
            }
        }
        List<String> l = new ArrayList<>();
        for (String s : l1) {
            if (MathUtils.isDouble(s)) {
                l.add("double:" + s);
            } else {
                if (s.startsWith(">")) {
                    String v = s.split(">", 2)[1];
                    if (MathUtils.isDouble(v)) {
                        l.add("biggerthan:" + v);
                    }
                } else if (s.startsWith("<")) {
                    String v = s.split("<", 2)[1];
                    if (MathUtils.isDouble(v)) {
                        l.add("smallerthan:" + v);
                    }
                }
            }
        }
        return l;
    }

    protected static boolean checkForGuiScale(String condition) {
        return checkForGuiScale(condition, Minecraft.getInstance().getWindow().getGuiScale());
    }

    private static boolean checkForGuiScale(String condition, double windowScale) {
        if (condition.startsWith("double:")) {
            String value = condition.replace("double:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale == valueScale);
        } else if (condition.startsWith("biggerthan:")) {
            String value = condition.replace("biggerthan:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale > valueScale);
        } else if (condition.startsWith("smallerthan:")) {
            String value = condition.replace("smallerthan:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale < valueScale);
        }
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.guiscale");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.guiscale.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.guiscale.value_name");
    }

    @Override
    public String getValuePreset() {
        return ">2.0";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
