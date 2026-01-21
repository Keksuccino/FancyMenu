package de.keksuccino.fancymenu.customization.requirement.requirements.system;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsInternetConnectionAvailableRequirement extends Requirement {

    public IsInternetConnectionAvailableRequirement() {
        super("is_internet_connection_available");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        return WebUtils.isInternetAvailable();

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_internet_connection_available");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_internet_connection_available.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.system");
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
