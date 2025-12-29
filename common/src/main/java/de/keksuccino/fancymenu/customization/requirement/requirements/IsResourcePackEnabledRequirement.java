package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsResourcePackEnabledRequirement extends Requirement {

    public IsResourcePackEnabledRequirement() {
        super("is_resource_pack_enabled");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value == null) {
            return false;
        }
        String target = value.trim();
        if (target.isEmpty()) {
            return false;
        }
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        for (Pack pack : repository.getSelectedPacks()) {
            String title = pack.getTitle().getString();
            if (title.equalsIgnoreCase(target) || pack.getId().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_resource_pack_enabled");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_resource_pack_enabled.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.is_resource_pack_enabled.value_name");
    }

    @Override
    public String getValuePreset() {
        return "Programmer Art";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
