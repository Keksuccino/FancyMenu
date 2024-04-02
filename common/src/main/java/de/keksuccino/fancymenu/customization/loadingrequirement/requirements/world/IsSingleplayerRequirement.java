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

public class IsSingleplayerRequirement extends LoadingRequirement {

    public IsSingleplayerRequirement() {
        super("fancymenu_loading_requirement_is_singpleplayer");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    //TODO übernehmen
    @Override
    public boolean isRequirementMet(@Nullable String value) {
        return WorldUtils.isSingleplayer();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.singleplayer");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.items.visibilityrequirements.singleplayer.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.world");
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
