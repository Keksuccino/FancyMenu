package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.gamemode;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsSurvivalRequirement extends LoadingRequirement {

    public IsSurvivalRequirement() {
        super("fancymenu_visibility_requirement_is_survival");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (Minecraft.getInstance().level != null) {
            LocalPlayer p = Minecraft.getInstance().player;
            ClientPacketListener l = Minecraft.getInstance().getConnection();
            if (l != null) {
                PlayerInfo playerinfo = l.getPlayerInfo(p.getGameProfile().getId());
                if (playerinfo != null) {
                    if (playerinfo.getGameMode() == GameType.SURVIVAL) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.visibilityrequirement.gamemode.is_survival");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.visibilityrequirement.gamemode.is_survival.desc"));
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
