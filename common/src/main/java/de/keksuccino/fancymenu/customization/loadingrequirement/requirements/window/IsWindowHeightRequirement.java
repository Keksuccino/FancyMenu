package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.window;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsWindowHeightRequirement extends LoadingRequirement {

    public IsWindowHeightRequirement() {
        super("fancymenu_loading_requirement_is_window_height");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value != null) {
            List<Integer> l = this.parseIntegers(value);
            if (!l.isEmpty()) {
                return l.contains(Minecraft.getInstance().getWindow().getHeight());
            }
        }
        return false;
    }

    protected List<Integer> parseIntegers(String value) {
        List<Integer> l = new ArrayList<>();
        if (value != null) {
            if (value.contains(",")) {
                for (String s : value.replace(" ", "").split("[,]")) {
                    //Filtering some human errors by checking for double, even if int is needed
                    if (MathUtils.isDouble(s)) {
                        l.add((int)Double.parseDouble(s));
                    }
                }
            } else {
                if (MathUtils.isInteger(value.replace(" ", ""))) {
                    l.add((int)Double.parseDouble(value.replace(" ", "")));
                }
            }
        }
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.windowheight");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.windowheight.desc", "" + Minecraft.getInstance().getWindow().getWidth(), "" + Minecraft.getInstance().getWindow().getHeight())));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.window");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.windowheight.valuename");
    }

    @Override
    public String getValuePreset() {
        return "1080";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
