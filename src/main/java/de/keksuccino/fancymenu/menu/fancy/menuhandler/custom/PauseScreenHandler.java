package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.mixin.client.IMixinPauseScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;

public class PauseScreenHandler extends MenuHandlerBase {

    public PauseScreenHandler() {
        super(PauseScreen.class.getName());
    }

    @Override
    protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {
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
