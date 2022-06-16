package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonMimeHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MenuCustomizationEvents {

	private static final Logger LOGGER = LogManager.getLogger("fanvymenu/MenuCustomizationEvents");
	
	private boolean idle = false;
	private boolean iconSetAfterFullscreen = false;
	private boolean scaleChecked = false;
	private boolean resumeWorldMusic = false;

	protected Screen lastScreen = null;

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onInitPrePre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (!ButtonCache.isCaching()) {
			if (MenuCustomization.isValidScreen(e.getGui())) {
				Screen current = Minecraft.getInstance().currentScreen;
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
				if (MenuCustomization.isNewMenu) {
//					LOGGER.info("###################### NEW SCREEN: " + this.lastScreen.getClass().getName());
					ButtonMimeHandler.clearCache();
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onMenuReloaded(MenuReloadedEvent e) {
		ButtonMimeHandler.clearCache();
		MenuCustomization.isNewMenu = true;
		this.lastScreen = null;
	}

	@SubscribeEvent(priority =  EventPriority.HIGH)
	public void onSoftReload(SoftMenuReloadEvent e) {
		ButtonMimeHandler.clearCache();
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
		if ((Minecraft.getInstance().world == null)) {
			if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
				Minecraft.getInstance().getMusicTicker().stop();
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
			if ((scale != -1) && (scale != 0)) {
				File f = FancyMenu.INSTANCE_DATA_DIR;
				if (!f.exists()) {
					f.mkdirs();
				}

				File f2 = new File(f.getPath() + "/default_scale_set.fm");
				File f3 = new File("mods/fancymenu/defaultscaleset.fancymenu");
				if (!f2.exists() && !f3.exists()) {
					try {
						f2.createNewFile();
						FileUtils.writeTextToFile(f2, false, "you're not supposed to be here! shoo!");
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					LOGGER.info("########################### FANCYMENU: SETTING DEFAULT GUI SCALE!");
					Minecraft.getInstance().gameSettings.guiScale = scale;
					Minecraft.getInstance().gameSettings.saveOptions();
					Minecraft.getInstance().updateWindowSize();
				}
			}
		}

		if (Minecraft.getInstance().currentScreen == null) {
			MenuCustomization.isCurrentScrollable = false;
		}
		
	}

	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Pre e) {
		MenuCustomization.isCurrentScrollable = true;
	}
	
}
