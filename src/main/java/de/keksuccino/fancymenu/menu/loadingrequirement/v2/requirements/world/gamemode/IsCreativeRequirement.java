package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.world.GameType;

import java.util.Arrays;
import java.util.List;

public class IsCreativeRequirement extends LoadingRequirement {

    public IsCreativeRequirement() {
        super("fancymenu_visibility_requirement_is_creative");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet( String value) {

        if (Minecraft.getMinecraft().world != null) {
            EntityPlayerSP p = Minecraft.getMinecraft().player;
            NetHandlerPlayClient l = Minecraft.getMinecraft().getConnection();
            if (l != null) {
                NetworkPlayerInfo playerinfo = l.getPlayerInfo(p.getGameProfile().getId());
                if (playerinfo != null) {
                    if (playerinfo.getGameType() == GameType.CREATIVE) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_creative");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.gamemode.is_creative.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.world");
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
