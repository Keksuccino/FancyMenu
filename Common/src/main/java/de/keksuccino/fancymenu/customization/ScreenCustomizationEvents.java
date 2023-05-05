package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.customization.button.ButtonMimeHandler;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.event.events.widget.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.event.events.ScreenReloadEvent;
import de.keksuccino.fancymenu.window.WindowHandler;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.konkrete.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScreenCustomizationEvents {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private boolean idle = false;
	private boolean iconSetAfterFullscreen = false;
	private boolean scaleChecked = false;
	private boolean resumeWorldMusic = false;
	protected Screen lastScreen = null;
	protected boolean fixedSelectWorldScreen = false;

	@EventListener
	public void onRenderScreenPost(RenderScreenEvent.Post e) {
		//Fix bugged Singleplayer menu in 1.19
		if ((e.getScreen() instanceof SelectWorldScreen) && !this.fixedSelectWorldScreen) {
			this.fixedSelectWorldScreen = true;
			Minecraft.getInstance().setScreen(e.getScreen());
		}
	}

	@EventListener(priority = EventPriority.HIGH)
	public void onModReloaded(ModReloadEvent e) {
		ButtonMimeHandler.clearCache();
		ScreenCustomization.isNewMenu = true;
		this.lastScreen = null;
	}

	@EventListener(priority =  EventPriority.HIGH)
	public void onSoftReload(ScreenReloadEvent e) {
		ButtonMimeHandler.clearCache();
		ScreenCustomization.isNewMenu = true;
		this.lastScreen = null;
	}

	@EventListener
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {

		if (!ButtonCache.isCaching()) {
			if (ScreenCustomization.isValidScreen(e.getScreen())) {
				Screen current = Minecraft.getInstance().screen;
				if (current != null) {
					if (this.lastScreen != null) {
						ScreenCustomization.isNewMenu = !this.lastScreen.getClass().getName().equals(current.getClass().getName());
					} else {
						ScreenCustomization.isNewMenu = true;
					}
				} else {
					ScreenCustomization.isNewMenu = true;
				}
				this.lastScreen = current;
				if (ScreenCustomization.isNewMenu) {
					ButtonMimeHandler.clearCache();
				}
			}
		}

		ScreenCustomization.isCurrentScrollable = false;

		if (ScreenCustomization.isValidScreen(e.getScreen()) && !LayoutEditorScreen.isActive) {
			this.idle = false;
		}
		if (ScreenCustomization.isValidScreen(e.getScreen()) && !ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen()) && !(e.getScreen() instanceof LayoutEditorScreen)) {
			ScreenCustomization.stopSounds();
			ScreenCustomization.resetSounds();
		}

		//Stopping menu music when deactivated in config
		if ((Minecraft.getInstance().level == null)) {
			if (!FancyMenu.getConfig().getOrDefault("playmenumusic", true)) {
				Minecraft.getInstance().getMusicManager().stopPlaying();
			}
		}

	}

	@EventListener
	public void onTick(ClientTickEvent.Pre e) {

		if (Minecraft.getInstance().screen == null) {
			this.lastScreen = null;
		}

		//Stopping audio for all menu handlers if no screen is being displayed
		if ((Minecraft.getInstance().screen == null) && !this.idle) {
			ScreenCustomization.stopSounds();
			ScreenCustomization.resetSounds();
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
				WindowHandler.updateWindowIcon();
				this.iconSetAfterFullscreen = true;
			}
		}

		if (!scaleChecked) {
			scaleChecked = true;
			
			int scale = FancyMenu.getConfig().getOrDefault("defaultguiscale", -1);
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

					LOGGER.info("[FANCYMENU] Setting default GUI scale..");
					Minecraft.getInstance().options.guiScale().set(scale);
					Minecraft.getInstance().options.save();
					Minecraft.getInstance().resizeDisplay();
				}
			}
		}

		if (Minecraft.getInstance().screen == null) {
			ScreenCustomization.isCurrentScrollable = false;
		}
		
	}

	@EventListener
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Pre e) {
		ScreenCustomization.isCurrentScrollable = true;
	}
	
}
