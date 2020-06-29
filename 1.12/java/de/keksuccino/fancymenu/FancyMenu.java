package de.keksuccino.fancymenu;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Field;

import de.keksuccino.core.config.Config;
import de.keksuccino.core.config.exceptions.InvalidValueException;
import de.keksuccino.core.filechooser.FileChooser;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.music.GameMusicHandler;
import de.keksuccino.fancymenu.menu.systemtray.FancyMenuTray;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "fancymenu", acceptedMinecraftVersions="[1.12,1.12.2]", dependencies = "after:findme")
public class FancyMenu {
	
	public static final String VERSION = "1.3.1";
	private static boolean isNotHeadless = false;
	
	public static Config config;
	
	private static File animationsPath = new File("config/fancymenu/animations");
	private static File customizationPath = new File("config/fancymenu/customization");
	
	public FancyMenu() {
		try {
			
			//Check if FancyMenu was loaded client- or serverside
			if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
	    		
	    		//Create all important directorys
	    		animationsPath.mkdirs();
	    		customizationPath.mkdirs();

	    		updateConfig();

	    		AnimationHandler.init();
	    		AnimationHandler.loadCustomAnimations();
	    		
	    		GameIntroHandler.init();
	    		
	        	MenuCustomization.init();
	        	
	        	PopupHandler.init();
	        	
	        	KeyboardHandler.init();
	        	
	        	SoundHandler.init();

	        	if (config.getOrDefault("enablehotkeys", true)) {
	        		Keybinding.init();
	        	}

	        	//Disabling file chooser and system tray if "FindMe" mod is active
	        	if (!Loader.isModLoaded("findme")) {
	        		isNotHeadless = this.escapeHeadless();
	        	}
	        	
	        	if (config.getOrDefault("enablesystemtray", true) && isRunningOnWindows() && isNotHeadless) {
	        		FancyMenuTray.init();
	        	}
	        	
	        	if (isNotHeadless) {
	        		FileChooser.init();
	        	}
	        	
	    	} else {
	    		System.out.println("## WARNING ## 'FancyMenu' is a client mod and has no effect when loaded on a server!");
	    	}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onInitPost(FMLPostInitializationEvent e) {
		try {
			if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
				MouseInput.init();
				
				Locals.init();
				
				GameMusicHandler.init();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	//If anyone knows how to do this in a less ugly way, PLEASE TELL ME..
	private boolean escapeHeadless() {
		try {
			System.setProperty("java.awt.headless", "false");
			System.setProperty("Djava.awt.headless", "false");
			System.setProperty("-Djava.awt.headless", "false");

			Field f = GraphicsEnvironment.class.getDeclaredField("headless");
			f.setAccessible(true);
			f.set(GraphicsEnvironment.getLocalGraphicsEnvironment(), false);

			Field f2 = Toolkit.class.getDeclaredField("toolkit");
			f2.setAccessible(true);
			f2.set(Toolkit.class, null);

			Toolkit.getDefaultToolkit();

			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public static boolean isRunningOnWindows() {
		String os = System.getProperty("os.name");
		if (os != null) {
			if (os.toLowerCase().startsWith("windows")) {
				return true;
			}
		}
		return false;
	}
	
	public static void updateConfig() {
    	try {
    		config = new Config("config/fancymenu/config.txt");
    		config.registerValue("enablesystemtray", true, "general", "ONLY AVAILABLE ON WINDOWS! A minecraft restart is required after changing this value.");
    		config.registerValue("playmenumusic", true, "general");
    		
    		config.registerValue("enablehotkeys", true, "general", "A minecraft restart is required after changing this value.");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
    		
			config.registerValue("hidebranding", true, "mainmenu");
			config.registerValue("hidelogo", false, "mainmenu");
			config.registerValue("showmainmenufooter", false, "mainmenu");
			config.registerValue("hiderealmsnotifications", false, "mainmenu");

			config.registerValue("hidesplashtext", false, "mainmenu_splash");
			config.registerValue("splashoffsetx", 0, "mainmenu_splash");
			config.registerValue("splashoffsety", 0, "mainmenu_splash");
			config.registerValue("splashrotation", -20, "mainmenu_splash");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("loadingscreendarkmode", false, "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			
			config.syncConfig();
			
			//Updating all categorys at start to keep them synchronized with older config files
			config.setCategory("enablesystemtray", "general");
			config.setCategory("enablehotkeys", "general");
			config.setCategory("playmenumusic", "general");
    		
			config.setCategory("showcustomizationbuttons", "customization");
			
			config.setCategory("hidebranding", "mainmenu");
			config.setCategory("hidelogo", "mainmenu");
			config.setCategory("showmainmenufooter", "mainmenu");
			config.setCategory("hiderealmsnotifications", "mainmenu");
			
			config.setCategory("hidesplashtext", "mainmenu_splash");
			config.setCategory("splashoffsetx", "mainmenu_splash");
			config.setCategory("splashoffsety", "mainmenu_splash");
			config.setCategory("splashrotation", "mainmenu_splash");
			
			config.setCategory("gameintroanimation", "loading");
			config.setCategory("loadingscreendarkmode", "loading");
			config.setCategory("showanimationloadingstatus", "loading");
			
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
	
	public static boolean isNotHeadless() {
		return isNotHeadless;
	}

}