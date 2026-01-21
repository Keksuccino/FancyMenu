package de.keksuccino.fancymenu.customization.requirement.requirements.system;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public class IsOsLinuxRequirement extends Requirement {

    public IsOsLinuxRequirement() {
        super("fancymenu_loading_requirement_is_os_linux");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        return !isMacOS() && !isWindows();

    }

    public static boolean isMacOS() {
        return Minecraft.ON_OSX;
    }

    public static boolean isWindows() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return (s.contains("win"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.oslinux");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.oslinux.desc");
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
