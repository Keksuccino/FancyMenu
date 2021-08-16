package de.keksuccino.fancymenu;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;

import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.button.VanillaButtonDescriptionHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.music.GameMusicHandler;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod("fancymenu")
public class FancyMenu {

	public static final String VERSION = "2.2.2";
	
	public static Config config;
	
	private static File animationsPath = new File("config/fancymenu/animations");
	private static File customizationPath = new File("config/fancymenu/customization");
	private static File customGuiPath = new File("config/fancymenu/customguis");
	private static File buttonscriptPath = new File("config/fancymenu/buttonscripts");
	private static File panoramaPath = new File("config/fancymenu/panoramas");
	private static File slideshowPath = new File("config/fancymenu/slideshows");

	private static boolean optifineLoaded = false;
	
	public FancyMenu() {
		try {

			ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
			
			//Check if FancyMenu was loaded client- or serverside
	    	if (FMLEnvironment.dist == Dist.CLIENT) {
	    		
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
	    		
	        	MenuCustomization.init();

	        	if (config.getOrDefault("enablehotkeys", true)) {
	        		Keybinding.init();
	        	}

	        	ButtonScriptEngine.init();

	        	VanillaButtonDescriptionHandler.init();

	        	Konkrete.addPostLoadingEvent("fancymenu", this::onClientSetup);

//				MinecraftForge.EVENT_BUS.register(new Test());
	        	
	    	} else {
	    		System.out.println("## WARNING ## 'FancyMenu' is a client mod and has no effect when loaded on a server!");
	    	}
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void onClientSetup() {
		try {

			initLocals();
			
	    	GameMusicHandler.init();

        	GuiConstructor.init();

        	try {
                Class.forName("optifine.Installer");
                optifineLoaded = true;
            }
            catch (ClassNotFoundException e) {}
	    	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void initLocals() {
		String baseresdir = "fmlocals/";
		File f = new File("config/fancymenu/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", baseresdir + "pt_br.local"), "pt_br", f.getPath());
		
		Locals.getLocalsFromDir(f.getPath());
	}

	public static void updateConfig() {
    	try {
    		config = new Config("config/fancymenu/config.txt");

    		config.registerValue("enablehotkeys", true, "general", "A minecraft restart is required after changing this value.");
    		config.registerValue("playmenumusic", true, "general");
    		config.registerValue("playbackgroundsounds", true, "general", "If menu background sounds added by FancyMenu should be played or not.");
    		config.registerValue("playbackgroundsoundsinworld", false, "general", "If menu background sounds added by FancyMenu should be played when a world is loaded.");
    		config.registerValue("stopworldmusicwhencustomizable", false, "general", "Stop vanilla world music when in a customizable menu.");
    		config.registerValue("defaultguiscale", -1, "general", "Sets the default GUI scale on first launch. Useful for modpacks. Cache data is saved in '/mods/fancymenu/'.");
    		config.registerValue("showdebugwarnings", true, "general");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
			config.registerValue("advancedmode", false, "customization");
    		
			config.registerValue("hidebranding", true, "mainmenu");
			config.registerValue("hidelogo", false, "mainmenu");
			config.registerValue("hiderealmsnotifications", false, "mainmenu");
			config.registerValue("copyrightposition", "bottom-right", "mainmenu");
			config.registerValue("hideforgenotifications", false, "mainmenu");
			config.registerValue("copyrightcolor", "#ffffff", "mainmenu");

			config.registerValue("hidesplashtext", false, "mainmenu_splash");
			config.registerValue("splashx", 0, "mainmenu_splash");
			config.registerValue("splashy", 0, "mainmenu_splash");
			config.registerValue("splashorientation", "original", "mainmenu_splash");
			config.registerValue("splashcolor", "#ffff00", "mainmenu_splash");
			config.registerValue("splashtextfile", "", "mainmenu_splash");
			config.registerValue("splashrotation", -20, "mainmenu_splash");
			
			config.registerValue("gameintroanimation", "", "loading");
			//TODO übernehmen
//			config.registerValue("loadingscreendarkmode", false, "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			config.registerValue("loadinganimationcolor", "#ffffffff", "loading");
			//TODO übernehmen
			config.registerValue("preloadanimations", false, "loading");

			config.registerValue("customwindowicon", false, "minecraftwindow", "A minecraft restart is required after changing this value.");
			config.registerValue("customwindowtitle", "", "minecraftwindow", "A minecraft restart is required after changing this value.");

			config.registerValue("showloadingscreenanimation", true, "world_loading_screen");
			config.registerValue("showloadingscreenpercent", true, "world_loading_screen");
			
			config.registerValue("showvanillamovewarning", true, "layouteditor", "If the warning when trying to move an vanilla button without an orientation should be displayed or not.");
			config.registerValue("editordeleteconfirmation", true, "layouteditor");

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

	public static boolean isOptifineLoaded() {
		return optifineLoaded;
	}

	public static boolean isDrippyLoadingScreenLoaded() {
		try {
			Class.forName("de.keksuccino.drippyloadingscreen.DrippyLoadingScreen");
			return true;
		} catch (Exception e) {}
		return false;
	}

}
