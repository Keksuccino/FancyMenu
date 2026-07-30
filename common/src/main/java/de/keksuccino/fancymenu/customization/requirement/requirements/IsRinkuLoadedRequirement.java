package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsRinkuLoadedRequirement extends Requirement {

    public IsRinkuLoadedRequirement() {
        super("is_rinku_loaded");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        return RinkuUtil.isRinkuLoaded() && RinkuUtil.rinku_initialized;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_rinku_loaded");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_rinku_loaded.desc");
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
