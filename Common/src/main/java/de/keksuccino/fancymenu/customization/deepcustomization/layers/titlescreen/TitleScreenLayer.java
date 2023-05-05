package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen;

import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.branding.TitleScreenBrandingElement;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.splash.TitleScreenSplashElement;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.logo.TitleScreenLogoElement;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.gui.screens.TitleScreen;

public class TitleScreenLayer extends DeepCustomizationLayer {

    public TitleScreenLayer() {

        super(TitleScreen.class.getName());

        this.registerElement(new TitleScreenLogoElement(this));
        this.registerElement(new TitleScreenBrandingElement(this));
        this.registerElement(new TitleScreenSplashElement(this));
        this.registerElement(new TitleScreenRealmsNotificationElement(this));

        Services.COMPAT.registerTitleScreenDeepCustomizationLayerElements(this);

    }

}
