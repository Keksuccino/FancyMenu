package de.keksuccino.fancymenu.customization.loadingrequirement.v2.requirements.system;

import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IsOsWindowsRequirement extends LoadingRequirement {

    public IsOsWindowsRequirement() {
        super("fancymenu_loading_requirement_is_os_windows");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        return isWindows();

    }

    protected static boolean isWindows() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return (s.contains("win"));
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.system");
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
