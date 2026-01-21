package de.keksuccino.fancymenu.customization.requirement.requirements.window;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.network.chat.Component;

public class IsWindowHeightBiggerThanRequirement extends Requirement {

    public IsWindowHeightBiggerThanRequirement() {
        super("fancymenu_loading_requirement_is_window_height_bigger_than");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value != null) {
            value = value.replace(" ", "");
            if (MathUtils.isDouble(value)) {
                return Minecraft.getInstance().getWindow().getHeight() > (int)Double.parseDouble(value);
            }
        }
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.windowheightbiggerthan");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.windowheightbiggerthan.desc", "" + Minecraft.getInstance().getWindow().getWidth(), "" + Minecraft.getInstance().getWindow().getHeight());
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.window");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.windowheightbiggerthan.value_name");
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
