package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora.AuroraDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats.BatsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.birds.BirdsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.browser.BrowserDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bubbles.BubblesDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.BuddyDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.butterflies.ButterfliesDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms.CherryBlossomsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.clouds.CloudsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs.CobwebsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti.ConfettiDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers.EmbersDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly.FireflyDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks.FireworksDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog.FogDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost.FrostDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.glsl.GlslDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves.LeavesDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth.OvergrowthDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain.RainDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.shootingstars.ShootingStarsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.skylanterns.SkyLanternsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow.SnowDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights.StringLightsDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams.SunbeamsDecorationOverlayBuilder;

public class DecorationOverlays {

    public static final SnowDecorationOverlayBuilder SNOW = new SnowDecorationOverlayBuilder();
    public static final RainDecorationOverlayBuilder RAIN = new RainDecorationOverlayBuilder();
    public static final FireflyDecorationOverlayBuilder FIREFLIES = new FireflyDecorationOverlayBuilder();
    public static final FireworksDecorationOverlayBuilder FIREWORKS = new FireworksDecorationOverlayBuilder();
    public static final StringLightsDecorationOverlayBuilder STRING_LIGHTS = new StringLightsDecorationOverlayBuilder();
    public static final LeavesDecorationOverlayBuilder LEAVES = new LeavesDecorationOverlayBuilder();
    public static final ConfettiDecorationOverlayBuilder CONFETTI = new ConfettiDecorationOverlayBuilder();
    public static final BuddyDecorationOverlayBuilder BUDDY = new BuddyDecorationOverlayBuilder();
    public static final BrowserDecorationOverlayBuilder BROWSER = new BrowserDecorationOverlayBuilder();
    public static final GlslDecorationOverlayBuilder GLSL = new GlslDecorationOverlayBuilder();
    public static final CherryBlossomsDecorationOverlayBuilder CHERRY_BLOSSOMS = new CherryBlossomsDecorationOverlayBuilder();
    public static final ShootingStarsDecorationOverlayBuilder SHOOTING_STARS = new ShootingStarsDecorationOverlayBuilder();
    public static final AuroraDecorationOverlayBuilder AURORA = new AuroraDecorationOverlayBuilder();
    public static final CloudsDecorationOverlayBuilder CLOUDS = new CloudsDecorationOverlayBuilder();
    public static final FogDecorationOverlayBuilder FOG = new FogDecorationOverlayBuilder();
    public static final SunbeamsDecorationOverlayBuilder SUNBEAMS = new SunbeamsDecorationOverlayBuilder();
    public static final SkyLanternsDecorationOverlayBuilder SKY_LANTERNS = new SkyLanternsDecorationOverlayBuilder();
    public static final ButterfliesDecorationOverlayBuilder BUTTERFLIES = new ButterfliesDecorationOverlayBuilder();
    public static final BirdsDecorationOverlayBuilder BIRDS = new BirdsDecorationOverlayBuilder();
    public static final BatsDecorationOverlayBuilder BATS = new BatsDecorationOverlayBuilder();
    public static final BubblesDecorationOverlayBuilder BUBBLES = new BubblesDecorationOverlayBuilder();
    public static final OvergrowthDecorationOverlayBuilder OVERGROWTH = new OvergrowthDecorationOverlayBuilder();
    public static final FrostDecorationOverlayBuilder FROST = new FrostDecorationOverlayBuilder();
    public static final CobwebsDecorationOverlayBuilder COBWEBS = new CobwebsDecorationOverlayBuilder();
    public static final EmbersDecorationOverlayBuilder EMBERS = new EmbersDecorationOverlayBuilder();

    /**
     * Overlays render in the order they get registered here, so each one renders in front of the ones registered before it.
     */
    public static void registerAll() {

        DecorationOverlayRegistry.register(BROWSER); // Browser should be the first one, so other overlays can render in front of it
        DecorationOverlayRegistry.register(GLSL); // Keep GLSL near the back so overlays can render in front when needed
        // The sky is the farthest thing an overlay can show, so other overlays render in front of it
        DecorationOverlayRegistry.register(SHOOTING_STARS);
        DecorationOverlayRegistry.register(AURORA);
        DecorationOverlayRegistry.register(CLOUDS);
        DecorationOverlayRegistry.register(BUDDY);
        DecorationOverlayRegistry.register(STRING_LIGHTS);
        DecorationOverlayRegistry.register(FIREWORKS);
        DecorationOverlayRegistry.register(SNOW);
        DecorationOverlayRegistry.register(RAIN);
        DecorationOverlayRegistry.register(FIREFLIES);
        DecorationOverlayRegistry.register(SKY_LANTERNS);
        DecorationOverlayRegistry.register(BUTTERFLIES);
        DecorationOverlayRegistry.register(BIRDS);
        DecorationOverlayRegistry.register(BATS);
        DecorationOverlayRegistry.register(BUBBLES);
        DecorationOverlayRegistry.register(LEAVES);
        DecorationOverlayRegistry.register(CHERRY_BLOSSOMS);
        DecorationOverlayRegistry.register(CONFETTI);
        DecorationOverlayRegistry.register(EMBERS);
        // Fog and light lie over the whole scene, the light shines through the fog
        DecorationOverlayRegistry.register(FOG);
        DecorationOverlayRegistry.register(SUNBEAMS);
        // Growth, webs and ice frame the screen itself, and frost sits on the glass in front of everything
        DecorationOverlayRegistry.register(OVERGROWTH);
        DecorationOverlayRegistry.register(COBWEBS);
        DecorationOverlayRegistry.register(FROST);

    }

}
