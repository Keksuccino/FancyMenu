package de.keksuccino.fancymenu.platform;

import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.fancymenu.platform.services.TitleScreenBrandingLineCollector;
import net.minecraft.network.chat.Component;
import net.minecraftforge.internal.BrandingControl;
import java.util.List;

public class ForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public List<Component> getTitleScreenBrandingLines() {
        return TitleScreenBrandingLineCollector.collectTopToBottom(BrandingControl::forEachLine, Component::literal);
    }

}
