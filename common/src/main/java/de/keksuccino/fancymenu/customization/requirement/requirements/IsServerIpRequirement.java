package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsServerIpRequirement extends Requirement {

    public IsServerIpRequirement() {
        super("fancymenu_visibility_requirement_is_server_ip");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            if (Minecraft.getInstance().level != null) {
                if (Minecraft.getInstance().getCurrentServer() != null) {
                    if (value.contains(":")) {
                        return Minecraft.getInstance().getCurrentServer().ip.equals(value);
                    } else {
                        String curIp = Minecraft.getInstance().getCurrentServer().ip;
                        if (curIp.contains(":")) {
                            curIp = curIp.split(":", 2)[0];
                        }
                        return curIp.equals(value);
                    }
                }
            }
        }

        return false;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_server_ip");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_server_ip.desc");
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.is_server_ip.value.desc");
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
