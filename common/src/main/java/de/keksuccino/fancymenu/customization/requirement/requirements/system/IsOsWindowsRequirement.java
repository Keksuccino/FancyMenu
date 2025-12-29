package de.keksuccino.fancymenu.customization.requirement.requirements.system;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IsOsWindowsRequirement extends Requirement {

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
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.oswindows");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.oswindows.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.system");
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
