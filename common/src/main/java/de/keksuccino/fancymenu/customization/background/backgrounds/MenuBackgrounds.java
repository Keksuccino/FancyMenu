package de.keksuccino.fancymenu.customization.background.backgrounds;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.backgrounds.color.ColorMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.panorama.PanoramaMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.slideshow.SlideshowMenuBackgroundBuilder;

public class MenuBackgrounds {

    public static final ImageMenuBackgroundBuilder IMAGE = new ImageMenuBackgroundBuilder();
    public static final SlideshowMenuBackgroundBuilder SLIDESHOW = new SlideshowMenuBackgroundBuilder();
    public static final PanoramaMenuBackgroundBuilder PANORAMA = new PanoramaMenuBackgroundBuilder();
    public static final ColorMenuBackgroundBuilder COLOR = new ColorMenuBackgroundBuilder();

    public static void registerAll() {

        MenuBackgroundRegistry.register(IMAGE);
        MenuBackgroundRegistry.register(SLIDESHOW);
        MenuBackgroundRegistry.register(PANORAMA);
        MenuBackgroundRegistry.register(COLOR);

    }

}
