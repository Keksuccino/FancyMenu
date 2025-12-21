package de.keksuccino.fancymenu.customization.screenoverlay.overlays;

import de.keksuccino.fancymenu.customization.screenoverlay.OverlayRegistry;
import de.keksuccino.fancymenu.customization.screenoverlay.overlays.snow.SnowOverlayBuilder;

public class Overlays {

    public static final SnowOverlayBuilder SNOW = new SnowOverlayBuilder();

    public static void registerAll() {

        OverlayRegistry.register(SNOW);

    }

}
