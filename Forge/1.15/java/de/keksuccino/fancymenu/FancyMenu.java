package de.keksuccino.fancymenu;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;

import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
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

	public static final String VERSION = "1.7.1";
	
	public static Config config;
	
	private static File animationsPath = new File("config/fancymenu/animations");
	private static File customizationPath = new File("config/fancymenu/customization");
	private static File customGuiPath = new File("config/fancymenu/customguis");
	private static File buttonscriptPath = new File("config/fancymenu/buttonscripts");
	private static File panoramaPath = new File("config/fancymenu/panoramas");
	private static File slideshowPath = new File("config/fancymenu/slideshows");
	
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
	    	
	    	MainWindowHandler.init();
        	MainWindowHandler.updateWindowIcon();
        	MainWindowHandler.updateWindowTitle();

        	GuiConstructor.init();
	    	
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
    		config.registerValue("softmode", false, "customization", "Maximizes mod compatibility. Disables background customization support for scrollable menu screens. Restart is needed after changing this value.");
    		config.registerValue("legacybuttonids", false, "customization");
    		
			config.registerValue("hidebranding", true, "mainmenu");
			config.registerValue("hidelogo", false, "mainmenu");
			config.registerValue("showmainmenufooter", false, "mainmenu");
			config.registerValue("hiderealmsnotifications", false, "mainmenu");
			config.registerValue("copyrightposition", "bottom-right", "mainmenu");
			config.registerValue("hideforgenotifications", false, "mainmenu");

			config.registerValue("hidesplashtext", false, "mainmenu_splash");
			config.registerValue("splashx", 0, "mainmenu_splash");
			config.registerValue("splashy", 0, "mainmenu_splash");
			config.registerValue("splashorientation", "original", "mainmenu_splash");
			config.registerValue("splashcolor", "#ffff00", "mainmenu_splash");
			config.registerValue("splashtextfile", "", "mainmenu_splash");
			config.registerValue("splashrotation", -20, "mainmenu_splash");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("loadingscreendarkmode", false, "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			config.registerValue("loadinganimationcolor", "#E22837", "loading");

			config.registerValue("customwindowicon", false, "minecraftwindow", "A minecraft restart is required after changing this value.");
			config.registerValue("customwindowtitle", "", "minecraftwindow", "A minecraft restart is required after changing this value.");
			
			config.registerValue("showloadingscreenanimation", true, "world_loading_screen");
			config.registerValue("showloadingscreenpercent", true, "world_loading_screen");

			config.registerValue("showvanillamovewarning", true, "layouteditor", "If the warning when trying to move an vanilla button without an orientation should be displayed or not.");
			config.registerValue("popupmenuscale", 1.0F, "layouteditor");
			
			config.syncConfig();
			
			//Updating all categorys at start to keep them synchronized with older config files
			config.setCategory("enablehotkeys", "general");
			config.setCategory("playmenumusic", "general");
			config.setCategory("playbackgroundsounds", "general");
			config.setCategory("playbackgroundsoundsinworld", "general");
			config.setCategory("stopworldmusicwhencustomizable", "general");
			config.setCategory("defaultguiscale", "general");
			config.setCategory("showdebugwarnings", "general");
			
			config.setCategory("showcustomizationbuttons", "customization");
			config.setCategory("softmode", "customization");
			config.setCategory("legacybuttonids", "customization");
			
			config.setCategory("hidebranding", "mainmenu");
			config.setCategory("hidelogo", "mainmenu");
			config.setCategory("showmainmenufooter", "mainmenu");
			config.setCategory("hiderealmsnotifications", "mainmenu");
			config.setCategory("copyrightposition", "mainmenu");
			config.setCategory("hideforgenotifications", "mainmenu");
			
			config.setCategory("hidesplashtext", "mainmenu_splash");
			config.setCategory("splashx", "mainmenu_splash");
			config.setCategory("splashy", "mainmenu_splash");
			config.setCategory("splashorientation", "mainmenu_splash");
			config.setCategory("splashcolor", "mainmenu_splash");
			config.setCategory("splashtextfile", "mainmenu_splash");
			config.setCategory("splashrotation", "mainmenu_splash");
			
			config.setCategory("gameintroanimation", "loading");
			config.setCategory("loadingscreendarkmode", "loading");
			config.setCategory("showanimationloadingstatus", "loading");
			config.setCategory("allowgameintroskip", "loading");
			config.setCategory("customgameintroskiptext", "loading");
			config.setCategory("loadinganimationcolor", "loading");

			config.setCategory("customwindowicon", "minecraftwindow");
			config.setCategory("customwindowtitle", "minecraftwindow");
			
			config.setCategory("showloadingscreenanimation", "world_loading_screen");
			config.setCategory("showloadingscreenpercent", "world_loading_screen");

			config.setCategory("showvanillamovewarning", "layouteditor");
			config.setCategory("popupmenuscale", "layouteditor");
			
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

}
