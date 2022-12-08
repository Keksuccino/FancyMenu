package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.fancymenu.api.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.commands.client.ClientExecutor;
import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import de.keksuccino.fancymenu.keybinding.Keybinding;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.button.VanillaButtonDescriptionHandler;
import de.keksuccino.fancymenu.menu.button.buttonactions.ButtonActions;
import de.keksuccino.fancymenu.menu.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.menu.placeholder.v1.placeholders.Placeholders;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.SetupSharingEngine;
import de.keksuccino.fancymenu.menu.fancy.item.items.CustomizationItems;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementHandler;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements.VisibilityRequirements;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.DeepCustomizationLayers;
import de.keksuccino.fancymenu.menu.fancy.music.GameMusicHandler;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import de.keksuccino.fancymenu.networking.Packets;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "fancymenu", acceptedMinecraftVersions="[1.12,1.12.2]", dependencies = "after:randompatches;after:findme;required-after:konkrete@[1.6.0,];required:forge@[14.23.5.2855,]", clientSideOnly = false)
public class FancyMenu {

	//TODO übernehmen
	public static final String VERSION = "2.13.0";
	public static final String MOD_LOADER = "forge";

	public static final Logger LOGGER = LogManager.getLogger("fancymenu/FancyMenu");

	public static Config config;

	//TODO übernehmen
	public static final File MOD_DIR = new File(getGameDirectory(), "/config/fancymenu");
	public static final File INSTANCE_DATA_DIR = new File(getGameDirectory(), "/fancymenu_data");
	public static final File INSTANCE_TEMP_DATA_DIR = new File(INSTANCE_DATA_DIR, "/temp");
	//---------------------

	//TODO übernehmen
	private static File animationsPath = new File(MOD_DIR, "/animations");
	private static File customizationPath = new File(MOD_DIR, "/customization");
	private static File customGuiPath = new File(MOD_DIR, "/customguis");
	private static File buttonscriptPath = new File(MOD_DIR, "/buttonscripts");
	private static File panoramaPath = new File(MOD_DIR, "/panoramas");
	private static File slideshowPath = new File(MOD_DIR, "/slideshows");
	//---------------------------
	
	public FancyMenu() {
		try {

			if (isClientSide()) {

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

				ClientExecutor.init();

				DeepCustomizationLayers.registerAll();

				ButtonActions.registerAll();

				VisibilityRequirements.registerAll();

				Placeholders.registerAll();

				de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.Placeholders.registerAll();

				CustomizationItems.registerAll();

				VariableHandler.init();

				ButtonIdentificator.init();

	    		AnimationHandler.init();
	    		AnimationHandler.loadCustomAnimations();
	    		
	    		PanoramaHandler.init();
	    		
	    		SlideshowHandler.init();
	    		
	    		CustomGuiLoader.loadCustomGuis();
	    		
	    		GameIntroHandler.init();

				VisibilityRequirementHandler.init();
	    		
	        	MenuCustomization.init();

				MenuBackgroundTypeRegistry.init();

	        	if (config.getOrDefault("enablehotkeys", true)) {
	        		Keybinding.init();
	        	}
	        	
	        	ButtonScriptEngine.init();

				LastWorldHandler.init();
	        	
	        	VanillaButtonDescriptionHandler.init();

	        	Konkrete.addPostLoadingEvent("fancymenu", this::onClientSetup);

				if (isOptifineCompatibilityMode()) {
					LOGGER.info("[FANCYMENU] Optifine compatibility mode enabled!");
				}

				LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in client-side mode!");

				if (FancyMenu.config.getOrDefault("allow_level_registry_interactions", false)) {
					LOGGER.info("[FANCYMENU] Level registry interactions allowed!");
				}
	        	
	    	} else {
				LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in server-side mode!");
	    	}

			Packets.registerAll();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isClientSide() {
		try {
			Class.forName("net.minecraft.client.Minecraft");
			return true;
		} catch (Exception e) {}
		return false;
	}

	@Mod.EventHandler
	public void onRegisterCommands(FMLPreInitializationEvent e) {

		if (isClientSide()) {
			ClientCommandHandler.instance.registerCommand(new OpenGuiScreenCommand());
			ClientCommandHandler.instance.registerCommand(new CloseGuiScreenCommand());
			VariableCommand.init();
			ClientCommandHandler.instance.registerCommand(new VariableCommand());
		}

	}

	@Mod.EventHandler
	public void onRegisterServerCommands(FMLServerStartingEvent e) {

		e.registerServerCommand(new ServerOpenGuiScreenCommand());
		e.registerServerCommand(new ServerCloseGuiScreenCommand());
		e.registerServerCommand(new ServerVariableCommand());

	}

	public void onClientSetup() {
		try {
			if (FMLClientHandler.instance().getSide() == Side.CLIENT) {

				initLocals();

				SetupSharingEngine.init();

				CustomLocalsHandler.loadLocalizations();
				
				GameMusicHandler.init();
	        	
	        	GuiConstructor.init();

				ServerCache.init();
	        	
			}
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
    		config.registerValue("stopworldmusicwhencustomizable", false, "general", "Stop vanilla world music when in a customizable menu.");
    		config.registerValue("defaultguiscale", -1, "general", "Sets the default GUI scale on first launch. Useful for modpacks. Cache data is saved in '/mods/fancymenu/'.");
    		config.registerValue("showdebugwarnings", true, "general");
			config.registerValue("forcefullscreen", false, "general");
    		
    		config.registerValue("showcustomizationbuttons", true, "customization");
			config.registerValue("advancedmode", false, "customization");

			config.registerValue("copyrightposition", "bottom-right", "mainmenu");
			config.registerValue("copyrightcolor", "#ffffff", "mainmenu");
			
			config.registerValue("gameintroanimation", "", "loading");
			config.registerValue("showanimationloadingstatus", true, "loading");
			config.registerValue("allowgameintroskip", true, "loading");
			config.registerValue("customgameintroskiptext", "", "loading");
			config.registerValue("loadinganimationcolor", "#E22837", "loading");
			config.registerValue("preloadanimations", true, "loading");
			
			config.registerValue("customwindowicon", false, "minecraftwindow", "A minecraft restart is required after changing this value.");
			config.registerValue("customwindowtitle", "", "minecraftwindow", "A minecraft restart is required after changing this value.");

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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static File getGameDirectory() {
		if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
			return Minecraft.getMinecraft().mcDataDir;
		} else {
			return new File("");
		}
	}

}
