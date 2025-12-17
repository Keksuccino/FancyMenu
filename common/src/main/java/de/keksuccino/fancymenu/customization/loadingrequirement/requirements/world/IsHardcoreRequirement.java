package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class IsHardcoreRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsHardcoreRequirement() {
        super("is_hardcore");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;
            return player.level().getLevelData().isHardcore();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
            return false;
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_hardcore");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_hardcore.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
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
