package de.keksuccino.fancymenu.customization.requirement.requirements.system;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

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
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.oswindows");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.oswindows.desc");
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
