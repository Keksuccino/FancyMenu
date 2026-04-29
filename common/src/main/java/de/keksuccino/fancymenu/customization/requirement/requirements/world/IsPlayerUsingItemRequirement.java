package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.chat.Component;

public class IsPlayerUsingItemRequirement extends Requirement {

    public IsPlayerUsingItemRequirement() {
        super("is_player_using_item");
    }

    public boolean hasValue() {
        return false;
    }

    public boolean isRequirementMet(@Nullable String value) {
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            return p.isUsingItem();
        }
        return false;
    }

    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_player_using_item");
    }

    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_player_using_item.desc");
    }

    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    public Component getValueDisplayName() {
        return null;
    }

    public String getValuePreset() {
        return null;
    }

    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}