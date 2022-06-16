package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.mixin.client.IMixinPauseScreen;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

public class PauseScreenHandler extends MenuHandlerBase {

    public PauseScreenHandler() {
        super(PauseScreen.class.getName());
    }

    @SubscribeEvent
    @Override
    public void onSoftReload(SoftMenuReloadEvent e) {
        super.onSoftReload(e);
    }

    @SubscribeEvent
    @Override
    public void onMenuReloaded(MenuReloadedEvent e) {
        super.onMenuReloaded(e);
    }

    @SubscribeEvent
    @Override
    public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
        super.onInitPre(e);
    }

    @Override
    protected void applyLayoutPre(PropertiesSection sec, GuiScreenEvent.InitGuiEvent.Pre e) {
        if (this.customizePauseScreen(e.getGui())) {
            super.applyLayoutPre(sec, e);
        }
    }

    @SubscribeEvent
    @Override
    public void onButtonsCached(ButtonCachedEvent e) {
        super.onButtonsCached(e);
    }

    @Override
    protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {
        if (this.customizePauseScreen(e.getGui())) {
            super.applyLayout(sec, renderOrder, e);
        }
    }

    @SubscribeEvent
    @Override
    public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
        super.onRenderPost(e);
    }

    @SubscribeEvent
    @Override
    public void drawToBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        super.drawToBackground(e);
    }

    @SubscribeEvent
    @Override
    public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
        super.onButtonClickSound(e);
    }

    @SubscribeEvent
    @Override
    public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
        super.onButtonRenderBackground(e);
    }

    @SubscribeEvent
    @Override
    public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
        super.onRenderListBackground(e);
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
