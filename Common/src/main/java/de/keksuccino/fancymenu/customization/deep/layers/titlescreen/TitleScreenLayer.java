package de.keksuccino.fancymenu.customization.deep.layers.titlescreen;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.branding.TitleScreenBrandingBuilder;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationBuilder;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.splash.TitleScreenSplashBuilder;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo.TitleScreenLogoBuilder;
import de.keksuccino.fancymenu.platform.Services;
import net.minecraft.client.gui.screens.TitleScreen;

public class TitleScreenLayer extends DeepScreenCustomizationLayer {

    public TitleScreenLayer() {

        super(TitleScreen.class.getName());

        this.registerBuilder(new TitleScreenLogoBuilder(this));
        this.registerBuilder(new TitleScreenBrandingBuilder(this));
        this.registerBuilder(new TitleScreenSplashBuilder(this));
        this.registerBuilder(new TitleScreenRealmsNotificationBuilder(this));

        Services.COMPAT.registerTitleScreenDeepCustomizationLayerElements(this);

    }

}
