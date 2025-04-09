package de.keksuccino.fancymenu.platform;

import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class FabricCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public List<Component> getTitleScreenBrandingLines() {
        String branding = "Minecraft " + SharedConstants.getCurrentVersion().getName();
        if (Minecraft.getInstance().isDemo()) {
            branding = branding + " Demo";
        } else {
            branding = branding + ("release".equalsIgnoreCase(Minecraft.getInstance().getVersionType()) ? "" : "/" + Minecraft.getInstance().getVersionType());
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            branding = branding + I18n.get("menu.modded");
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Components.literal(branding));
        return lines;
    }

}
