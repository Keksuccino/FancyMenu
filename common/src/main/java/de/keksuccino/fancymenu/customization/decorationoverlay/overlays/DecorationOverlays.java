package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly.FireflyDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain.RainDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow.SnowDecorationOverlayBuilder;

public class DecorationOverlays {

    public static final SnowDecorationOverlayBuilder SNOW = new SnowDecorationOverlayBuilder();
    public static final RainDecorationOverlayBuilder RAIN = new RainDecorationOverlayBuilder();
    public static final FireflyDecorationOverlayBuilder FIREFLIES = new FireflyDecorationOverlayBuilder();

    public static void registerAll() {

        DecorationOverlayRegistry.register(SNOW);
        DecorationOverlayRegistry.register(RAIN);
        DecorationOverlayRegistry.register(FIREFLIES);

    }

}
