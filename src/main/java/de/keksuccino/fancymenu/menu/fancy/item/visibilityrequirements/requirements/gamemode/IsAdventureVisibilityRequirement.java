package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsAdventureVisibilityRequirement extends VisibilityRequirement {

    public IsAdventureVisibilityRequirement() {
        super("fancymenu_visibility_requirement_is_adventure");
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
                    if (playerinfo.getGameMode() == GameType.ADVENTURE) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_adventure");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_adventure.desc"), "%n%"));
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
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
