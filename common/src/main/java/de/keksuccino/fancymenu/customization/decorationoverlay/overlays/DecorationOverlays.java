package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly.FireflyDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks.FireworksDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves.LeavesDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain.RainDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow.SnowDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights.StringLightsDecorationOverlayBuilder;

public class DecorationOverlays {

    public static final SnowDecorationOverlayBuilder SNOW = new SnowDecorationOverlayBuilder();
    public static final RainDecorationOverlayBuilder RAIN = new RainDecorationOverlayBuilder();
    public static final FireflyDecorationOverlayBuilder FIREFLIES = new FireflyDecorationOverlayBuilder();
    public static final FireworksDecorationOverlayBuilder FIREWORKS = new FireworksDecorationOverlayBuilder();
    public static final StringLightsDecorationOverlayBuilder STRING_LIGHTS = new StringLightsDecorationOverlayBuilder();
    public static final LeavesDecorationOverlayBuilder LEAVES = new LeavesDecorationOverlayBuilder();

    public static void registerAll() {

        DecorationOverlayRegistry.register(SNOW);
        DecorationOverlayRegistry.register(RAIN);
        DecorationOverlayRegistry.register(FIREFLIES);
        DecorationOverlayRegistry.register(FIREWORKS);
        DecorationOverlayRegistry.register(STRING_LIGHTS);
        DecorationOverlayRegistry.register(LEAVES);

    }

}
