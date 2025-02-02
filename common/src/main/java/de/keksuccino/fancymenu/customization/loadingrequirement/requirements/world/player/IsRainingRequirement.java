package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;

public class IsRainingRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsRainingRequirement() {
        super("is_raining");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if ((level != null) && (player != null)) {
                return isRainingAt(level, player.blockPosition());
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    private static boolean isRainingAt(@NotNull Level level, @NotNull BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        if (!level.isRaining()) return false;
        return (biome.getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.RAIN);
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.world.is_raining");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.world.is_raining.desc"));
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
