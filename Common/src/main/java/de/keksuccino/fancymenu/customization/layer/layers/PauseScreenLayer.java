package de.keksuccino.fancymenu.customization.layer.layers;

import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinPauseScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

public class PauseScreenLayer extends ScreenCustomizationLayer {

    public PauseScreenLayer() {
        super(PauseScreen.class.getName());
    }

    @Override
    protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCacheUpdatedEvent e) {
        if (this.customizePauseScreen(e.getScreen())) {
            super.applyLayout(sec, renderOrder, e);
        }
    }

    @Override
    protected void applyLayoutPre(PropertiesSection sec, InitOrResizeScreenEvent.Pre e) {
        if (this.customizePauseScreen(e.getScreen())) {
            super.applyLayoutPre(sec, e);
        }
    }

    protected boolean customizePauseScreen(Screen screen) {
        if (screen instanceof PauseScreen) {
            if (!((IMixinPauseScreen)screen).getShowPauseMenuFancyMenu()) {
                return false;
            } else {
            }
        }
        return true;
    }

}
