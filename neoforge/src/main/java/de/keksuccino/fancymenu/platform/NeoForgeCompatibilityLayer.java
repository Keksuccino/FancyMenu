package de.keksuccino.fancymenu.platform;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.copyright.TitleScreenForgeCopyrightBuilder;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.forge.top.TitleScreenForgeTopBuilder;
import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.internal.BrandingControl;
import java.util.ArrayList;
import java.util.List;

public class NeoForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public List<Component> getTitleScreenBrandingLines() {
        List<Component> lines = new ArrayList<>();
        BrandingControl.forEachLine(true, true, (brdline, brd) -> lines.add(Component.literal(brd)));
        return lines;
    }

    @Override
    public void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer) {
        layer.registerBuilder(new TitleScreenForgeCopyrightBuilder(layer));
        layer.registerBuilder(new TitleScreenForgeTopBuilder(layer));
    }

}
