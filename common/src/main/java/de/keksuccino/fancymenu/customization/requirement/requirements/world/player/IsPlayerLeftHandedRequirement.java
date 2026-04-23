package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.HumanoidArm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsPlayerLeftHandedRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsPlayerLeftHandedRequirement() {
        super("is_left_handed");
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
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if ((level != null) && (player != null)) {
                return (Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.world.is_left_handed");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.world.is_left_handed.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
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
