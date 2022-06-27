package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding.TitleScreenBrandingElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.logo.TitleScreenLogoElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash.TitleScreenSplashElement;
import net.minecraft.client.gui.screens.TitleScreen;

public class TitleScreenLayer extends DeepCustomizationLayer {

    public TitleScreenLayer() {

        super(TitleScreen.class.getName());

        this.registerElement(new TitleScreenLogoElement(this));
        this.registerElement(new TitleScreenBrandingElement(this));
        this.registerElement(new TitleScreenSplashElement(this));
        this.registerElement(new TitleScreenRealmsNotificationElement(this));

    }

}
