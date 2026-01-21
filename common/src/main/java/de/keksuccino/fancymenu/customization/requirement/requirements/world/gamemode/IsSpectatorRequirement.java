package de.keksuccino.fancymenu.customization.requirement.requirements.world.gamemode;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsSpectatorRequirement extends Requirement {

    public IsSpectatorRequirement() {
        super("fancymenu_visibility_requirement_is_spectator");
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

        if (Minecraft.getInstance().level != null) {
            LocalPlayer p = Minecraft.getInstance().player;
            ClientPacketListener l = Minecraft.getInstance().getConnection();
            if (l != null) {
                PlayerInfo playerinfo = l.getPlayerInfo(p.getGameProfile().getId());
                if (playerinfo != null) {
                    if (playerinfo.getGameMode() == GameType.SPECTATOR) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.gamemode.is_spectator");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.gamemode.is_spectator.desc");
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
