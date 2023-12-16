package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsServerIpRequirement extends LoadingRequirement {

    public IsServerIpRequirement() {
        super("fancymenu_visibility_requirement_is_server_ip");
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
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.visibilityrequirement.is_server_ip");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.visibilityrequirement.is_server_ip.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.visibilityrequirement.is_server_ip.value.desc");
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
