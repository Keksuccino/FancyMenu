package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.WorldUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsMultiplayerRequirement extends LoadingRequirement {

    public IsMultiplayerRequirement() {
        super("fancymenu_loading_requirement_is_multiplayer");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        return WorldUtils.isMultiplayer();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.multiplayer");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.multiplayer.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public String getValueDisplayName() {
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
