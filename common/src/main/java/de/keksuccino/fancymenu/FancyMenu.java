package de.keksuccino.fancymenu;

import java.io.File;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import net.minecraft.SharedConstants;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenu {

	//TODO Neues Resource System weiter machen und in Elemente und anderen Kram implementieren (besonders ResourceSupplier)

	//TODO allowWeb(boolean), allowLocal(boolean) und allowLocation(boolean) methoden in FileType
	// - In TextureHandler (und anderen handlern checken, ob file type den gewünschten Type unterstützt)

	//TODO Add ChooseResource Screen
	// - Screen nimmt MediaType als param
	// - Support für Web, Local und Location in einem Screen
	// - Web und Location haben auch Preview
	// - Web Input hat hint: " http://.. or https://.. "
	// - Web Input hat Placeholder Support

	//TODO Add support for FileTypes to FileChooser screens and show supported file types in Chooser GUI

	//TODO Bei Resource Reload (MC Resource Pack changed, etc.) alle resources per Resource#reload() reloaden

	//TODO Make progress bars smoother (calculate progress with float value)

	//TODO "Current Screen Identifier" placeholder

	//TODO Layout Listener System
	// - Auf Layout Ebene (rechtsklick editor back -> Listeners)
	// - Jeder Listener hat eine ExecutableBlock instanz, die er ausführen kann
	// - ExecutableBlock value placeholders nutzen, um values von Listeners an actions zu übergeben
	// - Listeners:
	//   - First Menu Open (wenn menu type das erste Mal in einer game session geöffnet wird) (checkt auf menu type von layout oder alle, wenn universal)
	//   - Open Menu (checkt auf menu type von layout oder alle, wenn universal)
	//   - Close Menu (checkt auf menu type von layout oder alle, wenn universal)
	//   - Init Or Re-Init Menu (InitOrResize)
	//   - Element Loaded (mit element identifier) -> dann bei ausführen von listener checken, ob erstelltes element == identifier
	//   - Key Typed (mit key name) -> bei ausführen check ob pressed key == given key
	//   - Mouse Clicked (mit mouseX, mouseY)
	//   - Mouse Moved (mit mouseX, mouseY)
	//   - Mouse Scrolled (mit scrollDelta)

	//TODO v2 Slider element fertig machen
	// - support für actions
	// - full background and handle texture customization (wie für Vanilla Slider)

	//TODO Button: Nine-Slice Background Mode (toggle on/off)
	// - Option, um Randbreite zu definieren, wenn möglich

	//TODO "Open Custom GUI" zu custom GUI context menu und Manage screen adden

	//TODO Einige der currently active layouts (nur enabled) als entries zu erster Ebene von Layout Context Menu adden
	// - Universal + Current Screen layouts kombinieren
	// - Nur 5-8 zeigen, dann entry "... (and X more)" (nicht clickable)

	//TODO Add short delay before elements can be moved in the editor (after left-clicking them)

	//TODO split Debug overlay into toggleable groups

	//TODO Keep context menus open when opening layout in system text editor, etc.

	//TODO Neue audio lib schreiben
	// - Melody
	// - Nutzt separate audio lib (nach lib suchen, die OGG unterstützt)
	// - Alle MC audio channel supporten
	// - Sollte nahtlos mit Channel volume eigenes volume ändern

	//TODO Simple Video Renderer schreiben
	// - Nach "Video Frame InputStream" googlen
	// - Evtl. mit Java-eigenen libs arbeiten

	//TODO Animations rewriten

	//TODO FIXEN: Neues element wird bei last mouse right-click pos geaddet, wenn man per menu bar addet (sollte oben links geaddet werden)
	// - Bei Menu Bar click cached mouse pos auf X30 Y30 setzen ??

	//TODO FIXEN: eventuell aktualisieren Forge Deep elements in title screen ihre position nicht richtig (checken)

	//TODO In layout safe-as screen als default file name universal identifiers nutzen, falls vorhanden

	//TODO add widget label scale option (per CustomizableWidget adden)

	//TODO Add Edit GUI to more Action values, if needed

	//TODO Testweise Title screen widget identifiers adden (eventuell dafür universal widget identifiers entfernen)

	//TODO "Split Text" placeholder (regex support)

	//TODO Markdown support for tooltips

	//TODO FIXEN: Splash Elemente werden bei resize reloaded (isNewMenu in builder fixen??)

	//TODO Render layout editor grid always at scale 1 (ignore game scale and UI scale)

	//TODO FIXEN: Manchmal wird per Anchor Overlay onHover der Anchor nicht aktualisiert (hover loading animation sichtbar, aber anchor wechselt am Ende nicht)
	// - Aufgetreten, nachdem Label von Custom Button editiert (fenster danach nicht resized oder gespeichert)
	// - Speichern per CTRL + S hat es gefixt (nur gespeichert, nicht resized, etc.)

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

		if (Services.PLATFORM.isOnClient()) {
			LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");
		} else {
			LOGGER.info("[FANCYMENU] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
		}

		FileTypes.registerAll();

		if (Services.PLATFORM.isOnClient()) {

			UIColorThemes.registerAll();

			TextColorFormatters.registerAll();

			ScreenCustomization.init();

			//TODO remove debug
			EventHandler.INSTANCE.registerListenersOf(new Test());

		}

		Compat.printInfoLog();

	}

	public static void lateClientInit() {

		LOGGER.info("[FANCYMENU] Starting late client initialization phase..");

		WindowHandler.updateCustomWindowIcon();

		WindowHandler.handleForceFullscreen();

		CursorHandler.init();

		CustomLocalsHandler.loadLocalizations();

		ServerCache.init();

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

	@Deprecated
	public static File getGameDirectory() {
		return GameDirectoryUtils.getGameDirectory();
	}

	private static File createDirectory(@NotNull File directory) {
		return FileUtils.createDirectory(directory);
	}

}
