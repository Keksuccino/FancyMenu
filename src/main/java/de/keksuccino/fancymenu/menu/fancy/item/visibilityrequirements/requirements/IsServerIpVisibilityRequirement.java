
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
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
            if (Minecraft.getMinecraft().world != null) {
                if (Minecraft.getMinecraft().getCurrentServerData() != null) {
                    if (value.contains(":")) {
                        return Minecraft.getMinecraft().getCurrentServerData().serverIP.equals(value);
                    } else {
                        String curIp = Minecraft.getMinecraft().getCurrentServerData().serverIP;
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
