package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.fancymenu.api.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.commands.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.events.CommandsRegisterEvent;
import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.button.VanillaButtonDescriptionHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.SetupSharingEngine;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementHandler;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.localization.Locals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenu implements ModInitializer {

	public static final String VERSION = "2.6.6";
	public static final String MOD_LOADER = "fabric";

	public static final Logger LOGGER = LogManager.getLogger("fancymenu/FancyMenu");

	public static Config config;

	public static final File MOD_DIR = new File("config/fancymenu");
	public static final File INSTANCE_DATA_DIR = new File("fancymenu_data");

	private static File animationsPath = new File(MOD_DIR.getPath() + "/animations");
	private static File customizationPath = new File(MOD_DIR.getPath() + "/customization");
	private static File customGuiPath = new File(MOD_DIR.getPath() + "/customguis");
	private static File buttonscriptPath = new File(MOD_DIR.getPath() + "/buttonscripts");
	private static File panoramaPath = new File(MOD_DIR.getPath() + "/panoramas");
	private static File slideshowPath = new File(MOD_DIR.getPath() + "/slideshows");
	
    @Override
    public void onInitialize() {

    	try {
			//Check if FancyMenu was loaded client- or serverside
    		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

				if (!MOD_DIR.isDirectory()) {
					MOD_DIR.mkdirs();
				}
				if (!INSTANCE_DATA_DIR.isDirectory()) {
					INSTANCE_DATA_DIR.mkdirs();
				}

	    		//Create all important directories
	    		animationsPath.mkdirs();
	    		customizationPath.mkdirs();
	    		customGuiPath.mkdirs();
	    		buttonscriptPath.mkdirs();
	    		panoramaPath.mkdirs();
	    		slideshowPath.mkdirs();

	    		updateConfig();

	    		AnimationHandler.init();
	    		AnimationHandler.loadCustomAnimations();
	    		
	    		PanoramaHandler.init();
	    		
	    		SlideshowHandler.init();
    		
	    		CustomGuiLoader.loadCustomGuis();
	    		
	    		GameIntroHandler.init();

				VisibilityRequirementHandler.init();
	    		
	        	MenuCustomization.init();

	        	if (config.getOrDefault("enablehotkeys", true)) {
	        		Keybinding.init();
	        	}
	        	
	        	ButtonScriptEngine.init();

				LastWorldHandler.init();
	        	
	        	VanillaButtonDescriptionHandler.init();

				MenuBackgroundTypeRegistry.init();
	        	
//	        	Konkrete.getEventHandler().registerEventsFrom(new Test());

	        	Konkrete.addPostLoadingEvent("fancymenu", this::onClientSetup);

				Konkrete.getEventHandler().registerEventsFrom(this);

//				MenuBackgroundTypeRegistry.registerBackgroundType(new ExampleMenuBackgroundType());
//				MenuBackgroundTypeRegistry.registerBackgroundType(new ExampleMenuBackgroundTypeWithInputString());

				if (isOptifineCompatibilityMode()) {
					LOGGER.info("Optifine compatibility mode!");
				}
	        	
	    	} else {
	    		LOGGER.warn("WARNING: FancyMenu is a client mod and has no effect when loaded on a server!");
	    	}
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }

	@SubscribeEvent
	public void onRegisterCommands(CommandsRegisterEvent e) {

		OpenGuiScreenCommand.register(e.getDispatcher());

	}

    
    private void onClientSetup() {
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
		
		Locals.getLocalsFromDir(f.getPath());
	}

    public static void updateConfig() {
    	try {

			config = new Config(MOD_DIR.getPath() + "/config.txt");

    		config.registerValue("enablehotkeys", true, "general", "A minecraft restart is required after changing this value.");
    		config.registerValue("playmenumusic", true, "general");
    		config.registerValue("playbackgroundsounds", true, "general", "If menu background sounds added by FancyMenu should be played or not.");
    		config.registerValue("playbackgroundsoundsinworld", false, "general", "If menu background sounds added by FancyMenu should be played when a world is loaded.");
    		config.registerValue("defaultguiscale", -1, "general", "Sets the default GUI scale on first launch. Useful for modpacks. Cache data is saved in '/mods/fancymenu/'.");
    		config.registerValue("showdebugwarnings", true, "general");
			config.registerValue("forcefullscreen", false, "general");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
			config.registerValue("advancedmode", false, "customization");
    		
			config.registerValue("hidebranding", true, "mainmenu");
			config.registerValue("hidelogo", false, "mainmenu");
			config.registerValue("hiderealmsnotifications", false, "mainmenu");
			if (FancyMenu.getMinecraftVersion().equals("1.18") || FancyMenu.getMinecraftVersion().equals("1.18.1")) {
				config.registerValue("copyrightposition", "bottom-right", "mainmenu");
			}
			if (FancyMenu.getMinecraftVersion().equals("1.18") || FancyMenu.getMinecraftVersion().equals("1.18.1")) {
				config.registerValue("copyrightcolor", "#ffffff", "mainmenu");
			}

			config.registerValue("hidesplashtext", false, "mainmenu_splash");
			config.registerValue("splashx", 0, "mainmenu_splash");
			config.registerValue("splashy", 0, "mainmenu_splash");
			config.registerValue("splashorientation", "original", "mainmenu_splash");
			config.registerValue("splashcolor", "#ffff00", "mainmenu_splash");
			config.registerValue("splashtextfile", "", "mainmenu_splash");
			config.registerValue("splashrotation", -20, "mainmenu_splash");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			config.registerValue("loadinganimationcolor", "#ffffffff", "loading");
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
			
			config.syncConfig();
			
			config.clearUnusedValues();

		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}
	
	public static File getAnimationPath() {
		if (!animationsPath.exists()) {
			animationsPath.mkdirs();
		}
		return animationsPath;
	}
	
	public static File getCustomizationPath() {
		if (!customizationPath.exists()) {
			customizationPath.mkdirs();
		}
		return customizationPath;
	}
	
	public static File getCustomGuiPath() {
		if (!customGuiPath.exists()) {
			customGuiPath.mkdirs();
		}
		return customGuiPath;
	}

	public static File getButtonScriptPath() {
		if (!buttonscriptPath.exists()) {
			buttonscriptPath.mkdirs();
		}
		return buttonscriptPath;
	}
	
	public static File getPanoramaPath() {
		if (!panoramaPath.exists()) {
			panoramaPath.mkdirs();
		}
		return panoramaPath;
	}

	public static File getSlideshowPath() {
		if (!slideshowPath.exists()) {
			slideshowPath.mkdirs();
		}
		return slideshowPath;
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
			Class.forName("de.keksuccino.drippyloadingscreen.DrippyLoadingScreen");
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static boolean isKonkreteLoaded() {
		try {
			Class.forName("de.keksuccino.konkrete.Konkrete");
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static boolean isAudioExtensionLoaded() {
		try {
			Class.forName("de.keksuccino.fmaudio.FmAudio");
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static String getMinecraftVersion() {
		return SharedConstants.getCurrentVersion().getReleaseTarget();
	}

}