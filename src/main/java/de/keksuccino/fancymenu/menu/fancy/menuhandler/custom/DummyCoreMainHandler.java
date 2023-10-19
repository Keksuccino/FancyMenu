package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;

public class DummyCoreMainHandler extends MainMenuHandler {
	
	@Override
	public String getMenuIdentifier() {
		return "DummyCore.Client.GuiMainMenuVanilla";
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		super.onInitPre(e);
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
	public void onRender(RenderScreenEvent.Pre e) {
		super.onRender(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(RenderScreenEvent.Post e) {
		super.onRenderPost(e);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPre(RenderScreenEvent.Pre e) {
		super.onRenderPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void drawToBackground(ScreenBackgroundRenderedEvent e) {
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
	public void onRenderListBackground(RenderListBackgroundEvent.Post e) {
		super.onRenderListBackground(e);
	}

}
