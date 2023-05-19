package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.server.ServerCache;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ServerData;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class IsServerOnlineRequirement extends LoadingRequirement {

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
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.serveronline");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.serveronline.desc")));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.serveronline.valuename");
    }

    @Override
    public String getValuePreset() {
        return "mc.mycoolserver.com";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
