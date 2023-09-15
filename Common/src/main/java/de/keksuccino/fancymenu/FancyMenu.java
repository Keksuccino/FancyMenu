package de.keksuccino.fancymenu;

import java.io.File;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenu {

	//TODO Render layout editor grid always at scale 1 (ignore game scale and UI scale)

	//TODO FIXEN: Manchmal wird per Anchor Overlay onHover der Anchor nicht aktualisiert (hover loading animation sichtbar, aber am ende passiert nichts)
	// - Aufgetreten, nachdem Label von Custom Button editiert (fenster danach nicht resized oder gespeichert)
	// - Speichern per CTRL + S hat es gefixt (nur gespeichert, nicht resized, etc.)

	//TODO Texte und anderen stuff in simplen Screens durch TextWidgets, etc. ersetzen (per Mixin), damit sie customized werden können
	// - Darauf achten, dass widgets immer selbe X Y pos haben müssen (sonst widget identifier nutzen)
	// - ProgressScreen
	// - LevelLoadingScreen (settings zum hiden von progress text entfernen) (eventuell auch chunk animation zu widget machen und dafür settings dafür entfernen)

	//TODO Add toggleable "Debug" menu overlay (when not in layout editor)
	// - Extendable !!
	// - Make toggleable with menu bar entry AND shortcut (CTRL + ALT + D)
	// - Shows screen identifier
	// - Shows screen width + height
	// - Active layout count for current screen (split universal and normal)
	// - Active element count for current screen (combine all active layouts)
	// - Active ticker elements count (split sync and async)
	// - Loaded Animations Count + Total Loaded Frames Count + Min/Max Frame Resolution used (warning if animation uses more than 200 frames at max 1080p)
	// - Loaded Slideshows Count + Total Loaded Slideshow Images Count + Min/Max Images Resolution used
	// - Shows current FPS in menu (render text orange if FPS <20 and red if <10)
	// - Shows RAM usage in menu (JVM ram usage and, if possible, actual system RAM usage) (render text orange or red if too much RAM used)
	// - Shows CPU + GPU usage in menu (render text orange or red if usage too high)
	// - draws borders around customizable vanilla/mod widgets
	//   - add simple right-click context menu for widgets with entries to copy locator, identifier, etc.

	//TODO Add markdown formatting code to set text font (use setFont() of components)

	//TODO placeholders und generic progress bar von Drippy porten (+ aus Drippy entfernen)

	//TODO Replace/Delete these classes:
	// - AutoScalingPopup
	// - FMPopup

	//TODO "Key Pressed" Loading Requirement
	// - Modes: ONCE_PER_KEY_PRESS (nur einen tick pro key press auf true), CONTINUOUS (hält bei key press dauerhaft auf true)
	// - Setzt "pressed" boolean bei onKeyPress auf true und bei onKeyRelease auf false (für modes)

	//TODO FIXEN: Layout Editor: grid toggle in Window tab wird nicht aktualisiert, wenn grid per Ctrl + G getoggelt wird

	//TODO Item Element, das per item meta (wie in give command) customized werden kann

	//TODO custom background support für slider adden (+ eventuell option zum changen von slider grabber textur)

	//TODO option für "keep position after anchor change" adden

	//TODO Key Presses in TextEditorScreen genau wie in LayoutEditorScreen handeln (pressed char getten und dann damit checken)

	//TODO Möglichkeit adden, Custom GUIs zu kopieren (bei kopieren öffnet sich input screen, um neuen identifier einzugeben)

	//TODO add setAlpha() support to Vanilla ImageButtons (language button, accessibility button)

	//TODO FIXEN: Slider elemente nutzen element opacity nicht (Vanilla bug oder in element vergessen?)

	//TODO Layout Editor: Toast Notifications rechts oben nach verschiedenen Aktionen wie copy/paste, undo/redo, etc.

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "3.0.0";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();
	public static final String MOD_ID = "fancymenu";

	public static final File MOD_DIR = createDirectory(new File(getGameDirectory(), "/config/fancymenu"));
	public static final File INSTANCE_DATA_DIR = createDirectory(new File(getGameDirectory(), "/fancymenu_data"));
	public static final File TEMP_DATA_DIR = createDirectory(new File(INSTANCE_DATA_DIR, "/.fancymenu_temp"));
	public static final File CACHE_DIR = createDirectory(new File(INSTANCE_DATA_DIR, "/cached_data"));

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

				UIColorThemes.registerAll();

				TextColorFormatters.registerAll();

				CursorHandler.init();

	        	ScreenCustomization.init();

				WindowHandler.handleForceFullscreen();

				//TODO remove debug
				EventHandler.INSTANCE.registerListenersOf(new Test());

	    	} else {
				LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
	    	}

			Compat.printInfoLog();
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void onClientSetup() {
		try {

			CustomLocalsHandler.loadLocalizations();

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
