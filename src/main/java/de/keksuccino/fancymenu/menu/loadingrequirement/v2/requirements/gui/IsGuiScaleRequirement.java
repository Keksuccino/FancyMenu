package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.gui;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsGuiScaleRequirement extends LoadingRequirement {

    public IsGuiScaleRequirement() {
        super("fancymenu_loading_requirement_is_gui_scale");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet( String value) {

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
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        double windowScale = res.getScaleFactor();
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
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.gui");
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.valuename");
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
