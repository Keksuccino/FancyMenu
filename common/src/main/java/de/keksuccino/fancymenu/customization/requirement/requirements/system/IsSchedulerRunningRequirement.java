package de.keksuccino.fancymenu.customization.requirement.requirements.system;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IsSchedulerRunningRequirement extends Requirement {

    public IsSchedulerRunningRequirement() {
        super("fancymenu_visibility_requirement_is_scheduler_running");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value == null) return false;
        String identifier = value.trim();
        if (identifier.isBlank()) return false;
        return SchedulerHandler.isRunning(identifier);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_scheduler_running");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_scheduler_running.desc");
    }

    @Override
    public @Nullable String getCategory() {
        return I18n.get("fancymenu.requirements.categories.system");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.is_scheduler_running.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "my_scheduler";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
