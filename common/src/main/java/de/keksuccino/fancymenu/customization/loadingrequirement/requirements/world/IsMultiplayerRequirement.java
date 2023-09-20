package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;

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

        return (Minecraft.getInstance().level != null) && (!Minecraft.getInstance().hasSingleplayerServer());

    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.multiplayer");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.multiplayer.desc")));
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
