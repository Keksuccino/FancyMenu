package de.keksuccino.fancymenu;

import java.io.File;

import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.guiconstruction.GuiConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenu {

	//PRIORITY:

	//TODO FIXEN: Hover sound von vanilla + custom buttons geht nicht


	//-----------------------------

	//TODO Arm rotation für player elements

	//TODO "SaveFileScreen" bauen, der genutzt wird, um layouts zu speichern
	// - Als Basis FileChooserScreen nutzen, aber Files können nicht "ausgewählt" werden, sondern sind nur sichtbar (damit man sieht, welche Files schon in Dir sind)
	// - Wenn kein Ordner in currentDir ausgewählt, dann File in currentDir speichern
	// - Wenn Ordner angewählt, File in angewähltem Ordner speichern
	// - Unter file list area ist Eingabefeld für File Name
	// - Klick auf "Done" returnt ZWEI FILES! (save directory UND file name)

	//TODO rewrite SetupSharingEngine
	// - All layout assets are now in config/fancymenu/assets, so just export config/fancymenu and that's it
	// - Pack exported setup to ZIP
	// - Allow import of ZIP setups
	// - Use SaveFileScreen to save setup

	//TODO re-implement GameIntroScreen

	//TODO Mixin für WorldLoadingScreen adden, um showAnimation und showPercent optionen zu handlen

	//TODO Key Presses in TextEditorScreen genau wie in LayoutEditorScreen handeln (pressed char getten und dann damit checken)

	//TODO Overlay durch schnelles, wiederholtes drücken von C togglen (Shortcut in Einstellungen entfernen; kann nicht deaktiviert werden)

	//TODO Bei 4K Auflösung UI Scale automatisch auf 2 bei erstem Start

	//TODO Alle Config Optionen auf Untermenüs von MenuBars aufteilen (kein Config Screen mehr)

	//TODO FIXEN: Nach ändern von anchor point springt das element manchmal beim ersten resizen (gesehen bei vanilla button)

	//TODO per action ermöglichen, zu disconnecten/welt zu verlassen und dabei den zielscreen zu wählen

	//TODO An inject point von TextColorFormatters (custom string color codes) system für HEX color codes adden ( format: §[#ffffff] / §[ffffff] )

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

	//TODO placeholders und generic progress bar von Drippy porten (+ aus Drippy entfernen)

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "3.0.0";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();
	public static final String MOD_ID = "fancymenu";

	public static final File MOD_DIR = createDirectory(new File(getGameDirectory(), "/config/fancymenu"));
	public static final File INSTANCE_DATA_DIR = createDirectory(new File(getGameDirectory(), "/.fancymenu_data"));
	public static final File TEMP_DATA_DIR = createDirectory(new File(INSTANCE_DATA_DIR, "/.fancymenu_temp"));
	public static final File CUSTOMIZATIONS_DIR = createDirectory(new File(MOD_DIR, "/customization"));
	public static final File ASSETS_DIR = createDirectory(new File(MOD_DIR, "/assets"));

	//TODO Move these to their actual classes
	private static final File ANIMATIONS_DIR = new File(MOD_DIR, "/animations");
	private static final File CUSTOM_GUIS_DIR = new File(MOD_DIR, "/customguis");
	private static final File BUTTONSCRIPT_DIR = new File(MOD_DIR, "/buttonscripts");
	private static final File PANORAMA_DIR = new File(MOD_DIR, "/panoramas");
	private static final File SLIDESHOW_DIR = new File(MOD_DIR, "/slideshows");

	private static Options options;

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

				UIColorThemes.registerAll();

				TextColorFormatters.registerAll();

	        	ScreenCustomization.init();

				WindowHandler.handleForceFullscreen();

				//TODO remove debug
				EventHandler.INSTANCE.registerListenersOf(new Test());

				if (isOptiFineLoaded()) {
					LOGGER.info("[FANCYMENU] OptiFine compatibility mode enabled!");
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

	public static Options getOptions() {
		if (options == null) {
			reloadOptions();
		}
		return options;
	}

	public static void reloadOptions() {
		options = new Options();
	}
	
	public static File getAnimationPath() {
		if (!ANIMATIONS_DIR.exists()) {
			ANIMATIONS_DIR.mkdirs();
		}
		return ANIMATIONS_DIR;
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
		try {
			Class.forName("optifine.Installer", false, FancyMenu.class.getClassLoader());
			return true;
		} catch (Exception ignored) {}
		return false;
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

	private static File createDirectory(@NotNull File directory) {
		return FileUtils.createDirectory(directory);
	}

}
