package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.server.ServerCache;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsServerOnlineRequirement extends Requirement {

    public IsServerOnlineRequirement() {
        super("fancymenu_loading_requirement_is_server_online");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            ServerData sd = ServerCache.getServer(value);
            return (sd != null) && (sd.ping != -1);
        }

        return false;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.serveronline");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.serveronline.desc");
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.serveronline.value_name");
    }

    @Override
    public String getValuePreset() {
        return "mc.exampleserver.com";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
