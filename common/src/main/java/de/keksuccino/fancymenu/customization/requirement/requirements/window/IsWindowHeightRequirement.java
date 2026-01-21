package de.keksuccino.fancymenu.customization.requirement.requirements.window;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsWindowHeightRequirement extends Requirement {

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
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.windowheight");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.windowheight.desc", "" + Minecraft.getInstance().getWindow().getWidth(), "" + Minecraft.getInstance().getWindow().getHeight());
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.window");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.windowheight.value_name");
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
