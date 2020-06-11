package de.keksuccino.fancymenu.menu.fancy;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

//Yes, I'm using too much "XYEvents" classes, but seperating events for every part of the mod helps me to find stuff more easily.
public class MenuCustomizationEvents {
	
	private boolean idle = false;
	private Screen lastScreen;
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Stopping audio for all menu handlers when changing the screen
		if (MenuCustomization.isValidScreen(e.getGui()) && !LayoutCreatorScreen.isActive) {
			if (this.lastScreen != e.getGui()) {
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
			}
			
			this.lastScreen = e.getGui();
			this.idle = false;
		}
		
		if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
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

}
