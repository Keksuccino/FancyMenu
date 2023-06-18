package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsLanguageRequirement extends LoadingRequirement {

    public IsLanguageRequirement() {
        super("fancymenu_loading_requirement_is_language");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            Minecraft.getInstance().options.languageCode.equals(value);
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.language");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.language.desc")));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.language.valuename");
    }

    @Override
    public String getValuePreset() {
        return "en_us";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
