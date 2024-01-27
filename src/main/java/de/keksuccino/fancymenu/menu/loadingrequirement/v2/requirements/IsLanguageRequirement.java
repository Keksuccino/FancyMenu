package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
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
    public boolean isRequirementMet( String value) {

        if (value != null) {
            return Minecraft.getInstance().options.languageCode.equals(value);
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.valuename");
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
