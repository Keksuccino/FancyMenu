package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;

//TODO übernehmen
public class IsPlayerWearingPumpkinRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsPlayerWearingPumpkinRequirement() {
        super("is_player_wearing_pumpkin");
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
                ItemStack itemStack = player.getInventory().getArmor(3);
                return (itemStack.is(Blocks.CARVED_PUMPKIN.asItem()));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.world.is_player_wearing_pumpkin");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.world.is_player_wearing_pumpkin.desc"));
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
