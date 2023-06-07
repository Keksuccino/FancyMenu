package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.fancymenu.rendering.ui.colorscheme.schemes.UIColorSchemes;
import de.keksuccino.fancymenu.window.WindowHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.config.Config;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenu {

	//PRIORITY:

	//TODO FIXEN: Hover sound von vanilla + custom buttons geht nicht


	//-----------------------------

	//TODO FIXEN: "is new Menu" stuff in ScreenCustomizationLayer und CustomizationHandler, etc. checkt nicht auf CustomGuiBase (wenn custom gui -> identifier getten)

	//TODO element list mit allen elementen eines layouts als widget (wie in photoshop)
	// - elemente sind nach order gelistet
	// - non-orderables haben andere Farbe und sind an richtiger (fester) position in list

	//TODO Möglichkeit adden, Custom GUIs zu kopieren (bei kopieren öffnet sich input screen, um neuen identifier einzugeben)

	//TODO move layout editor config stuff to separate config and make it editable via Menu Bar Tab in Editor

	//TODO add opacity support for title screen deep cuz elements (splash, logo, branding, realms icon, forge stuff)

	//TODO add setAlpha() support to Vanilla ImageButtons (language button, accessibility button)

	//TODO FIXEN: Slider elemente nutzen element opacity nicht (Vanilla bug oder in element vergessen?)

	//TODO label von custom buttons ist bei fade-in am Anfang kurz unsichtbar

	//TODO deep element customizations werden nicht in editor geladen, wenn editor neu geöffnet wird

	//TODO add "Select All" option to editor right-click menu and "Edit" tab in menu bar

	//TODO add cancel button to ManageRequirementsScreen that returns NULL as callback (NULL gets handled already, just needs the return logic)

	//TODO "Key Pressed" Loading Requirement

	//TODO Button Element: "Click On Key Press" option, um buttons per key press zu klicken

	//TODO Layout Editor: Toast Notifications rechts oben nach verschiedenen Aktionen wie copy/paste, undo/redo, etc.

	//TODO Text Element: auto line break (toggleable)

	//TODO ChooseMenuBackgroundScreen: background setzen scheint nicht zu gehen (getestet mit Image)

	//TODO Menu Bar in layout editor fixen

	//TODO loading screen fade-out fixen

	//TODO NOCH NICHT KOMPLETT: Alten Legacy/Deprecated/V1 stuff entfernen und komplett auf neue Registries porten
	// - Visibility Requirements (v1 stuff zu loading requirements machen)
	// - Actions

	//TODO Title Screen Mixins für customization adden
	// - BackgroundRenderEvent nach rendern von panorama back callen
	// - alle elemente entfernen, die durch Deep Cuz elemente ersetzt wurden

	//TODO "not_allowed.png" textur ersetzen

	//TODO File Picker GUI rewriten mit neuem Screen UI layout

	//TODO TextBox class, die multi-line text rendern kann + full markdown support (eventuell formatting system von TextEditor nutzen)

	//TODO GameIntro stuff komplett reworken/rewriten

	//TODO Alle dirs aus FancyMenu class nach MenuCustomization verschieben

	//TODO placeholders und generic progress bar von Drippy porten (+ aus Drippy entfernen)

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "3.0.0";
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

				UIColorSchemes.registerAll();

				TextColorFormatters.registerAll();

	        	ScreenCustomization.init();

				WindowHandler.handleForceFullscreen();

				//TODO remove debug
				EventHandler.INSTANCE.registerListenersOf(new Test());

				if (isOptiFineLoaded()) {
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

			SetupSharingHandler.init();

			CustomLocalsHandler.loadLocalizations();

        	GuiConstructor.init();

			ServerCache.init();
	    	
		} catch (Exception ex) {
			ex.printStackTrace();
		}

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

			config.registerValue("showgrid", true, "layouteditor");
			config.registerValue("gridsize", 10, "layouteditor");

			config.registerValue("uiscale", 1.0F, "ui");
			config.registerValue("show_unicode_warning", true, "ui");
			config.registerValue("play_ui_click_sounds", true, "ui");
			config.registerValue("light_mode", false, "ui");

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

	public static boolean isOptiFineLoaded() {
		return Konkrete.isOptifineLoaded;
	}

	public static boolean isAudioExtensionLoaded() {
		try {
			Class.forName("de.keksuccino.fmaudio.FmAudio", false, FancyMenu.class.getClassLoader());
			return true;
		} catch (Exception ignored) {}
		return false;
	}

	public static String getMinecraftVersion() {
		return SharedConstants.getCurrentVersion().getName();
	}

	public static File getGameDirectory() {
		if (Services.PLATFORM.isOnClient()) {
			return Minecraft.getInstance().gameDirectory;
		} else {
			return new File("");
		}
	}

}
