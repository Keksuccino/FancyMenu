package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.music.GameMusicHandler;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "fancymenu", acceptedMinecraftVersions="[1.12,1.12.2]", dependencies = "after:findme;after:konkrete")
public class FancyMenu {
	
	public static final String VERSION = "1.5.1";
	
	public static Config config;
	
	private static File animationsPath = new File("config/fancymenu/animations");
	private static File customizationPath = new File("config/fancymenu/customization");
	private static File customGuiPath = new File("config/fancymenu/customguis");
	private static File buttonscriptPath = new File("config/fancymenu/buttonscripts");
	
	public FancyMenu() {
		try {
			
			//Check if FancyMenu was loaded client- or serverside
			if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
	    		
	    		//Create all important directorys
	    		animationsPath.mkdirs();
	    		customizationPath.mkdirs();
	    		customGuiPath.mkdirs();
	    		buttonscriptPath.mkdirs();

	    		updateConfig();

	    		AnimationHandler.init();
	    		AnimationHandler.loadCustomAnimations();
	    		
	    		CustomGuiLoader.loadCustomGuis();
	    		
	    		GameIntroHandler.init();
	    		
	        	MenuCustomization.init();

	        	if (config.getOrDefault("enablehotkeys", true)) {
	        		Keybinding.init();
	        	}
	        	
	        	ButtonScriptEngine.init();
	        	
	        	Konkrete.addPostLoadingEvent("fancymenu", this::onClientSetup);
	        	
	    	} else {
	    		System.out.println("## WARNING ## 'FancyMenu' is a client mod and has no effect when loaded on a server!");
	    	}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onClientSetup() {
		try {
			if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
				initLocals();
				
				GameMusicHandler.init();
				
				MainWindowHandler.init();
	        	MainWindowHandler.updateWindowIcon();
	        	MainWindowHandler.updateWindowTitle();
	        	
	        	GuiConstructor.init();
	        	
			}
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
			config.registerValue("splashoffsetx", 0, "mainmenu_splash");
			config.registerValue("splashoffsety", 0, "mainmenu_splash");
			config.registerValue("splashrotation", -20, "mainmenu_splash");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			
			config.registerValue("customwindowicon", false, "minecraftwindow", "A minecraft restart is required after changing this value.");
			config.registerValue("customwindowtitle", "", "minecraftwindow", "A minecraft restart is required after changing this value.");
			
			config.registerValue("showundoredocontrols", false, "layouteditor", "If the undo/redo control buttons of the layout editor should be visible or not.");
			config.registerValue("showvanillamovewarning", true, "layouteditor", "If the warning when trying to move an vanilla button without an orientation should be displayed or not.");
			
			config.syncConfig();
			
			//Updating all categorys at start to keep them synchronized with older config files
			config.setCategory("enablehotkeys", "general");
			config.setCategory("playmenumusic", "general");
			config.setCategory("playbackgroundsounds", "general");
    		
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
			config.setCategory("splashoffsetx", "mainmenu_splash");
			config.setCategory("splashoffsety", "mainmenu_splash");
			config.setCategory("splashrotation", "mainmenu_splash");
			
			config.setCategory("gameintroanimation", "loading");
			config.setCategory("showanimationloadingstatus", "loading");
			config.setCategory("allowgameintroskip", "loading");
			config.setCategory("customgameintroskiptext", "loading");

			config.setCategory("customwindowicon", "minecraftwindow");
			config.setCategory("customwindowtitle", "minecraftwindow");
			
			config.setCategory("showundoredocontrols", "layouteditor");
			config.setCategory("showvanillamovewarning", "layouteditor");
			
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

}
