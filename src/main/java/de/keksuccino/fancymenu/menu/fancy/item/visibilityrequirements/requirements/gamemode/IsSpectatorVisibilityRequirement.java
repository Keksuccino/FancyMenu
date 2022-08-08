//---
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.gamemode;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.world.GameType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsSpectatorVisibilityRequirement extends VisibilityRequirement {

    public IsSpectatorVisibilityRequirement() {
        super("fancymenu_visibility_requirement_is_spectator");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (Minecraft.getMinecraft().world != null) {
            EntityPlayerSP p = Minecraft.getMinecraft().player;
            NetHandlerPlayClient l = Minecraft.getMinecraft().getConnection();
            if (l != null) {
                NetworkPlayerInfo playerinfo = l.getPlayerInfo(p.getGameProfile().getId());
                if (playerinfo != null) {
                    if (playerinfo.getGameType() == GameType.SPECTATOR) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_spectator");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_spectator.desc"), "%n%"));
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
