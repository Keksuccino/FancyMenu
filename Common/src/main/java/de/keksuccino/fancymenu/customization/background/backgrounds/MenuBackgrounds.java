package de.keksuccino.fancymenu.customization.background.backgrounds;

import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.backgrounds.animation.AnimationMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.panorama.PanoramaMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.slideshow.SlideshowMenuBackgroundBuilder;

public class MenuBackgrounds {

    public static void registerAll() {

        //TODO screen for layout editor to set background
        // - ScrollArea in new GUI style with all background types
        // - if background type already active, show button "Edit"
        // - if background type is not active, show button "Set"

        MenuBackgroundRegistry.register(new ImageMenuBackgroundBuilder());
        MenuBackgroundRegistry.register(new AnimationMenuBackgroundBuilder());
        MenuBackgroundRegistry.register(new SlideshowMenuBackgroundBuilder());
        MenuBackgroundRegistry.register(new PanoramaMenuBackgroundBuilder());

    }

}
