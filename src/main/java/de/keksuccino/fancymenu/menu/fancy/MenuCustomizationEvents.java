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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MenuCustomizationEvents {

	private static final Logger LOGGER = LogManager.getLogger("fancymenu/MenuCustomizationEvents");
	
	private boolean idle = false;
	private boolean iconSetAfterFullscreen = false;
	private boolean scaleChecked = false;
	private boolean resumeWorldMusic = false;

	protected Screen lastScreen = null;

	protected boolean fixedSelectWorldScreen = false;

	@SubscribeEvent
	public void onRenderScreenPost(ScreenEvent.Render.Post e) {

		//Fix bugged Singleplayer menu in 1.19
		if ((e.getScreen() instanceof SelectWorldScreen) && !this.fixedSelectWorldScreen) {
			this.fixedSelectWorldScreen = true;
			Minecraft.getInstance().setScreen(e.getScreen());
		}

	}

	//I don't fucking know why I made a "PrePre" event, but even if it's ugly, it works, so I will just not touch it anymore lmao
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onInitPrePre(ScreenEvent.Init.Pre e) {

		if (!ButtonCache.isCaching()) {
			if (MenuCustomization.isValidScreen(e.getScreen())) {
				Screen current = Minecraft.getInstance().screen;
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
	public void onInitPre(ScreenEvent.Init.Pre e) {

		MenuCustomization.isCurrentScrollable = false;
		
		if (MenuCustomization.isValidScreen(e.getScreen()) && !LayoutEditorScreen.isActive) {
			this.idle = false;
		}
		if (MenuCustomization.isValidScreen(e.getScreen()) && !MenuCustomization.isMenuCustomizable(e.getScreen()) && !(e.getScreen() instanceof LayoutEditorScreen)) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
		}

		//Stopping menu music when deactivated in config
		if ((Minecraft.getInstance().level == null)) {
			if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
				Minecraft.getInstance().getMusicManager().stopPlaying();
			}
		}

	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {

		if (Minecraft.getInstance().screen == null) {
			this.lastScreen = null;
		}

		//Stopping audio for all menu handlers if no screen is being displayed
		if ((Minecraft.getInstance().screen == null) && !this.idle) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
			this.idle = true;
		}

		if ((Minecraft.getInstance().level != null) && (Minecraft.getInstance().screen == null) && this.resumeWorldMusic) {
			Minecraft.getInstance().getSoundManager().resume();
			this.resumeWorldMusic = false;
		}

		if (Minecraft.getInstance().getWindow().isFullscreen()) {
			this.iconSetAfterFullscreen = false;
		} else {
			if (!this.iconSetAfterFullscreen) {
				MainWindowHandler.updateWindowIcon();
				this.iconSetAfterFullscreen = true;
			}
		}

		if (!scaleChecked && (Minecraft.getInstance().options != null)) {
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
					Minecraft.getInstance().options.guiScale().set(scale);
					Minecraft.getInstance().options.save();
					Minecraft.getInstance().resizeDisplay();
				}
			}
		}

		if (Minecraft.getInstance().screen == null) {
			MenuCustomization.isCurrentScrollable = false;
		}
		
	}

	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Pre e) {
		MenuCustomization.isCurrentScrollable = true;
	}
	
}
