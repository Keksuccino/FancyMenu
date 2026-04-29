package de.keksuccino.fancymenu.customization.background.backgrounds;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.backgrounds.browser.BrowserMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.color.ColorMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.glsl.GlslMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.panorama.PanoramaMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.slideshow.SlideshowMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef.MCEFVideoMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo.NativeVideoMenuBackgroundBuilder;

public class MenuBackgrounds {

    public static final ImageMenuBackgroundBuilder IMAGE = new ImageMenuBackgroundBuilder();
    public static final SlideshowMenuBackgroundBuilder SLIDESHOW = new SlideshowMenuBackgroundBuilder();
    public static final PanoramaMenuBackgroundBuilder PANORAMA = new PanoramaMenuBackgroundBuilder();
    public static final ColorMenuBackgroundBuilder COLOR = new ColorMenuBackgroundBuilder();
    public static final BrowserMenuBackgroundBuilder BROWSER = new BrowserMenuBackgroundBuilder();
    public static final GlslMenuBackgroundBuilder GLSL = new GlslMenuBackgroundBuilder();
    public static final NativeVideoMenuBackgroundBuilder VIDEO = new NativeVideoMenuBackgroundBuilder();
    public static final MCEFVideoMenuBackgroundBuilder VIDEO_MCEF = new MCEFVideoMenuBackgroundBuilder();

    public static void registerAll() {

        // Registering COLOR first is important to be able to use it as fallback for other background types or just to have a filler for backgrounds with transparent parts
        MenuBackgroundRegistry.register(COLOR); // add first to always have a way to set a simple and save background for fallback
        MenuBackgroundRegistry.register(PANORAMA);
        MenuBackgroundRegistry.register(IMAGE);
        MenuBackgroundRegistry.register(SLIDESHOW);
        MenuBackgroundRegistry.register(VIDEO);
        MenuBackgroundRegistry.register(VIDEO_MCEF);
        MenuBackgroundRegistry.register(BROWSER); // add as late as possible
        MenuBackgroundRegistry.register(GLSL); // add last

    }

}
