package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.events.SubscribeEvent;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiMenuHandlerBase  extends MenuHandlerBase {

	public CustomGuiMenuHandlerBase(String identifier) {
		super(identifier);
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
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		super.onInitPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(RenderScreenEvent.Post e) {
		super.onRenderPost(e);
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
	
	@Override
	protected boolean shouldCustomize(Screen menu) {
		if (menu instanceof CustomGuiBase) {
			if (((CustomGuiBase) menu).getIdentifier().equals(this.getMenuIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
