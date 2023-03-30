package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.world.gamemode;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.world.GameType;

import java.util.Arrays;
import java.util.List;

public class IsAdventureRequirement extends LoadingRequirement {

    public IsAdventureRequirement() {
        super("fancymenu_visibility_requirement_is_adventure");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet( String value) {

        if (Minecraft.getInstance().level != null) {
            ClientPlayerEntity p = Minecraft.getInstance().player;
            ClientPlayNetHandler l = Minecraft.getInstance().getConnection();
            if (l != null) {
                NetworkPlayerInfo playerinfo = l.getPlayerInfo(p.getGameProfile().getId());
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
