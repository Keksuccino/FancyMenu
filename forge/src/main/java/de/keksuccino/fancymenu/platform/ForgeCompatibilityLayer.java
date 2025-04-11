package de.keksuccino.fancymenu.platform;

import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import net.minecraftforge.internal.BrandingControl;
import java.util.ArrayList;
import java.util.List;

public class ForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public List<Component> getTitleScreenBrandingLines() {
        List<Component> lines = new ArrayList<>();
        BrandingControl.forEachLine(true, true, (brdline, brd) -> lines.add(Components.literal(brd)));
        return lines;
    }

}
