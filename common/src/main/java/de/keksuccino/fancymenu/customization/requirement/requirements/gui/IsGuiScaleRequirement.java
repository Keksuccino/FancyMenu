package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;

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

        if (value != null) {
            List<String> l = this.parseValues(value);
            if (!l.isEmpty()) {
                for (String s : l) {
                    if (!checkForGuiScale(s)) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;

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
        double windowScale = Minecraft.getInstance().getWindow().getGuiScale();
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
