package de.keksuccino.drippyloadingscreen;

import java.io.File;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrippyLoadingScreen implements ModInitializer {

	public static final String VERSION = "2.2.5";
	public static final String MOD_LOADER = "fabric";
	
	public static final File MOD_DIR = new File(FancyMenu.getGameDirectory(), "/config/drippyloadingscreen");

	public static final Logger LOGGER = LogManager.getLogger();
	
	public static Config config;

	public DrippyLoadingScreen() {

		//Initializing animation engine early (only needed in Fabric)
		LOGGER.info("[DRIPPY LOADING SCREEN] Force-initializing FancyMenu's animation engine..");
		AnimationHandler.loadCustomAnimations();

	}

	@Override
	public void onInitialize() {

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

			if (!MOD_DIR.exists()) {
				MOD_DIR.mkdirs();
			}

			initConfig();

			Konkrete.getEventHandler().registerEventsFrom(new EventHandler());

			Konkrete.addPostLoadingEvent("drippyloadingscreen", this::onClientSetup);

		} else {
			LOGGER.warn("WARNING: 'Drippy Loading Screen' is a client mod and has no effect when loaded on a server!");
		}

	}
	
	private void onClientSetup() {
		try {

			//do stuff
	    	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void initConfig() {
		if (config == null) {
			updateConfig();
		}
	}
	
	public static void updateConfig() {
		
		try {
			
			config = new Config(MOD_DIR.getPath() + "/config.cfg");

			config.registerValue("allow_universal_layouts", false, "general");
			config.registerValue("early_fade_out_elements", true, "general");
			
			config.syncConfig();
			
			config.clearUnusedValues();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
