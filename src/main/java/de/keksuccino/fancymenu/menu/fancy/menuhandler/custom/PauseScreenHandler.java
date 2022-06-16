package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.mixin.client.IMixinPauseScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;

public class PauseScreenHandler extends MenuHandlerBase {

    public PauseScreenHandler() {
        super(IngameMenuScreen.class.getName());
    }

    @Override
    protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {
        if (this.customizePauseScreen(e.getGui())) {
            super.applyLayout(sec, renderOrder, e);
        }
    }

    @Override
    protected void applyLayoutPre(PropertiesSection sec, GuiScreenEvent.InitGuiEvent.Pre e) {
        if (this.customizePauseScreen(e.getGui())) {
            super.applyLayoutPre(sec, e);
        }
    }

    protected boolean customizePauseScreen(Screen screen) {
        if (screen instanceof IngameMenuScreen) {
            if (!((IMixinPauseScreen)screen).getShowPauseMenuFancyMenu()) {
                return false;
            } else {
            }
        }
        return true;
    }

}
