package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow.SnowDecorationOverlayBuilder;

public class DecorationOverlays {

    public static final SnowDecorationOverlayBuilder SNOW = new SnowDecorationOverlayBuilder();

    public static void registerAll() {

        DecorationOverlayRegistry.register(SNOW);

    }

}
