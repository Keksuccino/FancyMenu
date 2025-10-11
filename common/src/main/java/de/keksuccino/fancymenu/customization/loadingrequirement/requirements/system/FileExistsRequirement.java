package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.system;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileExistsRequirement extends LoadingRequirement {

    public FileExistsRequirement() {
        super("fancymenu_loading_requirement_file_exists");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value != null) {
            // Convert short .minecraft paths to actual valid .minecraft paths
            value = DotMinecraftUtils.resolveMinecraftPath(value);
            return new File(value).exists();
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.file_exists");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.file_exists.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.system");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.file_exists.value_name");
    }

    @Override
    public String getValuePreset() {
        return "path/to/file_or_folder";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
