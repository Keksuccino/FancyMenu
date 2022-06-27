//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class IsServerIpVisibilityRequirement extends VisibilityRequirement {

    public IsServerIpVisibilityRequirement() {
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
                            curIp = curIp.split("[:]", 2)[0];
                        }
                        return curIp.equals(value);
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip.desc"), "%n%"));
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "mc.mycoolserver.com";
    }

    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}
