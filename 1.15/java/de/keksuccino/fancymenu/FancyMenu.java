package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.config.Config;
import de.keksuccino.config.exceptions.InvalidValueException;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("fancymenu")
public class FancyMenu {
	
	public static Config config;
	
	private static File animationsPath = new File("config/fancymenu/animations");
	private static File customizationPath = new File("config/fancymenu/customization");
	
	public FancyMenu() {
		//Checking if FancyMain was loaded client- or serverside
    	if (FMLEnvironment.dist == Dist.CLIENT) {
    		
    		//Creating all important directorys
    		animationsPath.mkdirs();
    		customizationPath.mkdirs();

    		initConfig();

    		AnimationHandler.init();
    		AnimationHandler.loadCustomAnimations();
    		
        	MenuCustomization.init();
        	
    	} else {
    		System.out.println("## WARNING ## 'FancyMain' is a client mod and has no effect when loaded on a server!");
    	}
	}
	
	private static void initConfig() {
    	try {
    		config = new Config("config/fancymenu/config.txt");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
    		
			config.registerValue("hidebranding", true, "mainmenu");
			config.registerValue("hidelogo", true, "mainmenu");
			config.registerValue("buttonfadein", true, "mainmenu", "When a background animation is defined, the buttons can start fading in at a specific animation frame.");
			config.registerValue("mainmenufadeinframe", 60, "mainmenu", "Sets the animation frame at which the main menu buttons should start fading in.");
			config.registerValue("hidesplashtext", true, "mainmenu");
			config.registerValue("showmainmenufooter", true, "mainmenu");
			config.syncConfig();
			
			//Updating all categorys at start to keep it synchronized with older config files
			config.setCategory("showcustomizationbuttons", "customization");
			
			config.setCategory("hidebranding", "mainmenu");
			config.setCategory("hidelogo", "mainmenu");
			config.setCategory("buttonfadein", "mainmenu");
			config.setCategory("mainmenufadeinframe", "mainmenu");
			config.setCategory("hidesplashtext", "mainmenu");
			config.setCategory("showmainmenufooter", "mainmenu");
			
			config.clearUnusedValues();
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}
	
	public static File getAnimationPath() {
		return animationsPath;
	}
	
	public static File getCustomizationPath() {
		return customizationPath;
	}

}
