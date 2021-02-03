package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuCustomizationEvents {
	
	private boolean idle = false;
	private boolean iconSetAfterFullscreen = false;
	private boolean scaleChecked = false;
	private boolean resumeWorldMusic = false;
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		
		if (MenuCustomization.isValidScreen(e.getGui()) && !LayoutCreatorScreen.isActive) {
			this.idle = false;
		}
		if (MenuCustomization.isValidScreen(e.getGui()) && !MenuCustomization.isMenuCustomizable(e.getGui()) && !(e.getGui() instanceof LayoutCreatorScreen)) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
		}

		//Stopping menu music when deactivated in config
		if ((Minecraft.getInstance().world == null)) {
			if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
				Minecraft.getInstance().getMusicTicker().stop();
			}
		} else {
			if (MenuCustomization.isMenuCustomizable(e.getGui()) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false)) {
				Minecraft.getInstance().getSoundHandler().pause();
				this.resumeWorldMusic = true;
			}
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

		if ((Minecraft.getInstance().world != null) && (Minecraft.getInstance().currentScreen == null) && this.resumeWorldMusic) {
			Minecraft.getInstance().getSoundHandler().resume();
			this.resumeWorldMusic = false;
		}

		if (Minecraft.getInstance().getMainWindow().isFullscreen()) {
			this.iconSetAfterFullscreen = false;
		} else {
			if (!this.iconSetAfterFullscreen) {
				MainWindowHandler.updateWindowIcon();
				this.iconSetAfterFullscreen = true;
			}
		}

		if (!scaleChecked && (Minecraft.getInstance().gameSettings != null)) {
			scaleChecked = true;
			
			int scale = FancyMenu.config.getOrDefault("defaultguiscale", -1);
			if (scale != -1) {
				File f = new File("mods/fancymenu");
				if (!f.exists()) {
					f.mkdirs();
				}
				
				File f2 = new File(f.getPath() + "/defaultscaleset.fancymenu");
				if (!f2.exists()) {
					try {
						f2.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					Minecraft.getInstance().gameSettings.guiScale = scale;
					Minecraft.getInstance().gameSettings.saveOptions();
					Minecraft.getInstance().updateWindowSize();
				}
			}
		}
	}
	
}
