package de.keksuccino.fancymenu.menu.fancy;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuCustomizationEvents {
	
	private boolean idle = false;
	//TODO übernehmen
//	private Screen lastScreen;
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (MenuCustomization.isValidScreen(e.getGui()) && !LayoutCreatorScreen.isActive) {
			//TODO übernehmen
//			if (this.lastScreen != e.getGui()) {
//				MenuCustomization.stopSounds();
//				MenuCustomization.resetSounds();
//			}
//			
//			this.lastScreen = e.getGui();
			
			this.idle = false;
		}
		//TODO übernehmen
		if (!MenuCustomization.isValidScreen(Minecraft.getInstance().currentScreen)) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
		}
		
		//Stopping menu music when deactivated in config
		if ((Minecraft.getInstance().world == null) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			Minecraft.getInstance().getMusicTicker().stop();
		}
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all menu handlers if no screen is being displayed
		if ((Minecraft.getInstance().currentScreen == null) && !this.idle) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
			this.idle = true;
		}
	}

	@SubscribeEvent
	public void onMenuReload(MenuReloadedEvent e) {
		MenuCustomization.reloadExcludedMenus();
	}
}
