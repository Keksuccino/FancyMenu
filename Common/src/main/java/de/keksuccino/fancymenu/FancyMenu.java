package de.keksuccino.fancymenu;

import java.io.File;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.api.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.window.WindowHandler;
import de.keksuccino.fancymenu.customization.backend.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.backend.setupsharing.SetupSharingEngine;
import de.keksuccino.fancymenu.customization.backend.server.ServerCache;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import de.keksuccino.fancymenu.customization.backend.MenuCustomization;
import de.keksuccino.fancymenu.customization.backend.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.localization.Locals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenu {

	//TODO Neue Widget Tooltip Engine
	//TODO Altes Tooltip rendering fixen (am besten mit neuer engine ersetzen)

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "2.14.7";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();
	public static final String MOD_ID = "fancymenu";

	public static final File MOD_DIR = new File(getGameDirectory(), "/config/fancymenu");
	public static final File INSTANCE_DATA_DIR = new File(getGameDirectory(), "/fancymenu_data");
	public static final File INSTANCE_TEMP_DATA_DIR = new File(INSTANCE_DATA_DIR, "/temp");

	private static final File ANIMATIONS_DIR = new File(MOD_DIR, "/animations");
	private static final File CUSTOMIZATIONS_DIR = new File(MOD_DIR, "/customization");
	private static final File CUSTOM_GUIS_DIR = new File(MOD_DIR, "/customguis");
	private static final File BUTTONSCRIPT_DIR = new File(MOD_DIR, "/buttonscripts");
	private static final File PANORAMA_DIR = new File(MOD_DIR, "/panoramas");
	private static final File SLIDESHOW_DIR = new File(MOD_DIR, "/slideshows");

	private static Config config;

	public static void init() {

		try {

	    	if (Services.PLATFORM.isOnClient()) {

				LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");

				if (!MOD_DIR.isDirectory()) {
					MOD_DIR.mkdirs();
				}
				if (!INSTANCE_DATA_DIR.isDirectory()) {
					INSTANCE_DATA_DIR.mkdirs();
				}
	    		
	    		//Create all important directories
	    		ANIMATIONS_DIR.mkdirs();
	    		CUSTOMIZATIONS_DIR.mkdirs();
	    		CUSTOM_GUIS_DIR.mkdirs();
	    		BUTTONSCRIPT_DIR.mkdirs();
	    		PANORAMA_DIR.mkdirs();
	    		SLIDESHOW_DIR.mkdirs();
	    		
	        	MenuCustomization.init();

				WindowHandler.handleForceFullscreen();

				MenuBackgroundTypeRegistry.init();

//				EventHandler.INSTANCE.registerListenersOf(new Test());

				if (isOptifineCompatibilityMode()) {
					LOGGER.info("[FANCYMENU] OptiFine compatibility mode enabled!");
				}

				if (FancyMenu.getConfig().getOrDefault("allow_level_registry_interactions", false)) {
					LOGGER.info("[FANCYMENU] Level registry interactions allowed!");
				}

	    	} else {
				LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
	    	}
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void onClientSetup() {

		try {

			initLocals();

			SetupSharingEngine.init();

			CustomLocalsHandler.loadLocalizations();

        	GuiConstructor.init();

			ServerCache.init();
	    	
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private static void initLocals() {

		String baseresdir = "fmlocals/";
		File f = new File(MOD_DIR.getPath() + "/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "pt_br.local"), "pt_br", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "zh_cn.local"), "zh_cn", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "uk_ua.local"), "uk_ua", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "ru_ru.local"), "ru_ru", f.getPath());
		
		Locals.getLocalsFromDir(f.getPath());

	}

	public static Config getConfig() {
		initConfig();
		return config;
	}
	
	public static void initConfig() {
		if (config == null) {
			updateConfig();
		}
	}

	public static void updateConfig() {
    	try {

    		config = new Config(MOD_DIR.getAbsolutePath().replace("\\", "/") + "/config.txt");

    		config.registerValue("enablehotkeys", true, "general", "A minecraft restart is required after changing this value.");
    		config.registerValue("playmenumusic", true, "general");
    		config.registerValue("playbackgroundsounds", true, "general", "If menu background sounds added by FancyMenu should be played or not.");
    		config.registerValue("playbackgroundsoundsinworld", false, "general", "If menu background sounds added by FancyMenu should be played when a world is loaded.");
    		config.registerValue("defaultguiscale", -1, "general", "Sets the default GUI scale on first launch. Useful for modpacks. Cache data is saved in '/mods/fancymenu/'.");
    		config.registerValue("showdebugwarnings", true, "general");
			config.registerValue("forcefullscreen", false, "general");
			config.registerValue("variables_to_reset_on_launch", "", "general");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
			config.registerValue("advancedmode", false, "customization");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			config.registerValue("preloadanimations", false, "loading");

			config.registerValue("customwindowicon", false, "minecraftwindow", "A minecraft restart is required after changing this value.");
			config.registerValue("customwindowtitle", "", "minecraftwindow", "A minecraft restart is required after changing this value.");

			config.registerValue("showloadingscreenanimation", true, "world_loading_screen");
			config.registerValue("showloadingscreenpercent", true, "world_loading_screen");

			config.registerValue("show_server_icons", true, "multiplayer_screen");

			config.registerValue("show_world_icons", true, "singleplayer_screen");
			
			config.registerValue("showvanillamovewarning", true, "layouteditor", "If the warning when trying to move an vanilla button without an orientation should be displayed or not.");
			config.registerValue("editordeleteconfirmation", true, "layouteditor");
			config.registerValue("showgrid", false, "layouteditor");
			config.registerValue("gridsize", 10, "layouteditor");

			config.registerValue("uiscale", 1.0F, "ui");
			config.registerValue("show_unicode_warning", true, "ui");

			config.registerValue("allow_level_registry_interactions", true, "compatibility");
			
			config.syncConfig();
			
			config.clearUnusedValues();

		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}
	
	public static File getAnimationPath() {
		if (!ANIMATIONS_DIR.exists()) {
			ANIMATIONS_DIR.mkdirs();
		}
		return ANIMATIONS_DIR;
	}
	
	public static File getCustomizationsDirectory() {
		if (!CUSTOMIZATIONS_DIR.exists()) {
			CUSTOMIZATIONS_DIR.mkdirs();
		}
		return CUSTOMIZATIONS_DIR;
	}
	
	public static File getCustomGuisDirectory() {
		if (!CUSTOM_GUIS_DIR.exists()) {
			CUSTOM_GUIS_DIR.mkdirs();
		}
		return CUSTOM_GUIS_DIR;
	}

	public static File getButtonScriptPath() {
		if (!BUTTONSCRIPT_DIR.exists()) {
			BUTTONSCRIPT_DIR.mkdirs();
		}
		return BUTTONSCRIPT_DIR;
	}

	public static File getPanoramaDirectory() {
		if (!PANORAMA_DIR.exists()) {
			PANORAMA_DIR.mkdirs();
		}
		return PANORAMA_DIR;
	}

	public static File getSlideshowDirectory() {
		if (!SLIDESHOW_DIR.exists()) {
			SLIDESHOW_DIR.mkdirs();
		}
		return SLIDESHOW_DIR;
	}

	@Deprecated
	public static boolean isOptifineLoaded() {
		return isOptifineCompatibilityMode();
	}

	public static boolean isOptifineCompatibilityMode() {
		return Konkrete.isOptifineLoaded;
	}

	public static boolean isDrippyLoadingScreenLoaded() {
		try {
			Class.forName("de.keksuccino.drippyloadingscreen.DrippyLoadingScreen", false, FancyMenu.class.getClassLoader());
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static boolean isKonkreteLoaded() {
		try {
			Class.forName("de.keksuccino.konkrete.Konkrete", false, FancyMenu.class.getClassLoader());
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static String getMinecraftVersion() {
		return SharedConstants.getCurrentVersion().getName();
	}

	public static boolean isAudioExtensionLoaded() {
		try {
			Class.forName("de.keksuccino.fmaudio.FmAudio", false, FancyMenu.class.getClassLoader());
			return true;
		} catch (Exception ignored) {}
		return false;
	}

	public static File getGameDirectory() {
		if (Services.PLATFORM.isOnClient()) {
			return Minecraft.getInstance().gameDirectory;
		} else {
			return new File("");
		}
	}

}
