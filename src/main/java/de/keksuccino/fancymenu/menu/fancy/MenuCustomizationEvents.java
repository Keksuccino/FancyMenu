package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class MenuCustomizationEvents {
	
	private boolean idle = false;
	private boolean iconSetAfterFullscreen = false;
	private boolean scaleChecked = false;
	private boolean resumeWorldMusic = false;
	
	protected Screen lastScreen = null;
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onInitPrePre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (!ButtonCache.isCaching()) {
			if (MenuCustomization.isValidScreen(e.getGui())) {
				Screen current = MinecraftClient.getInstance().currentScreen;
				if (current != null) {
					if (this.lastScreen != null) {
						if (!this.lastScreen.getClass().getName().equals(current.getClass().getName())) {
							MenuCustomization.isNewMenu = true;
						} else {
							MenuCustomization.isNewMenu = false;
						}
					} else {
						MenuCustomization.isNewMenu = true;
					}
				} else {
					MenuCustomization.isNewMenu = true;
				}
				this.lastScreen = current;
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onMenuReloaded(MenuReloadedEvent e) {
		MenuCustomization.isNewMenu = true;
		this.lastScreen = null;
	}

	@SubscribeEvent(priority =  EventPriority.HIGH)
	public void onSoftReload(SoftMenuReloadEvent e) {
		MenuCustomization.isNewMenu = true;
		this.lastScreen = null;
	}
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		
		MenuCustomization.isCurrentScrollable = false;
		
		if (MenuCustomization.isValidScreen(e.getGui()) && !LayoutEditorScreen.isActive) {
			this.idle = false;
		}
		if (MenuCustomization.isValidScreen(e.getGui()) && !MenuCustomization.isMenuCustomizable(e.getGui()) && !(e.getGui() instanceof LayoutEditorScreen)) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
		}
		
		//Stopping menu music when deactivated in config
		if ((MinecraftClient.getInstance().world == null)) {
			if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
				MinecraftClient.getInstance().getMusicTracker().stop();
			}
		} else {
			if (MenuCustomization.isMenuCustomizable(e.getGui()) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false)) {
				MinecraftClient.getInstance().getSoundManager().pauseAll();
				this.resumeWorldMusic = true;
			}
		}
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all menu handlers if no screen is being displayed
		if ((MinecraftClient.getInstance().currentScreen == null) && !this.idle) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
			this.idle = true;
		}
		
		if ((MinecraftClient.getInstance().world != null) && (MinecraftClient.getInstance().currentScreen == null) && this.resumeWorldMusic) {
			MinecraftClient.getInstance().getSoundManager().resumeAll();
			this.resumeWorldMusic = false;
		}
		
		if (MinecraftClient.getInstance().getWindow().isFullscreen()) {
			this.iconSetAfterFullscreen = false;
		} else {
			if (!this.iconSetAfterFullscreen) {
				MainWindowHandler.updateWindowIcon();
				this.iconSetAfterFullscreen = true;
			}
		}
		
		if (!scaleChecked && (MinecraftClient.getInstance().options != null)) {
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
					
					MinecraftClient.getInstance().options.guiScale = scale;
					MinecraftClient.getInstance().options.write();
					MinecraftClient.getInstance().onResolutionChanged();
				}
			}
		}
		
		if (MinecraftClient.getInstance().currentScreen == null) {
			MenuCustomization.isCurrentScrollable = false;
		}
		
	}
	
	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Pre e) {
		MenuCustomization.isCurrentScrollable = true;
	}
	
}
