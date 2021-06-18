package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent.Pre;

public class MoreRefinedStorageMainHandler extends MainMenuHandler {
	
	@Override
	public String getMenuIdentifier() {
		//PLEASE don't do stuff like overriding a whole screen just to alter the splash text, thanks.
		return "be.nevoka.morerefinedstorage.gui.GuiCustomMainMenu";
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(Pre e) {
		super.onInitPre(e);
	}
	
	//TODO neu in 1.17
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
	public void onRender(DrawScreenEvent.Pre e) {
		super.onRender(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(DrawScreenEvent.Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
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

}
