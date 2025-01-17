package de.keksuccino.fancymenu;

import java.io.File;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.text.color.colors.TextColorFormatters;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import net.minecraft.SharedConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenu {

	//TODO Parallax Image Menu Background
	//TODO Fixen: Button sound cut off
	//TODO Item element

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "3.4.0";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();
	public static final String MOD_ID = "fancymenu";

	public static final File MOD_DIR = createDirectory(new File(GameDirectoryUtils.getGameDirectory(), "/config/fancymenu"));
	public static final File INSTANCE_DATA_DIR = createDirectory(new File(GameDirectoryUtils.getGameDirectory(), "/fancymenu_data"));
	public static final File TEMP_DATA_DIR = ObjectUtils.build(() -> {
		File f = new File(INSTANCE_DATA_DIR, "/fancymenu_temp");
		if (f.isDirectory()) org.apache.commons.io.FileUtils.deleteQuietly(f);
		return createDirectory(f);
	});
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

			if (MCEFUtil.isMCEFLoaded()) BrowserHandler.init();

			UIColorThemes.registerAll();

			TextColorFormatters.registerAll();

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

	private static File createDirectory(@NotNull File directory) {
		return FileUtils.createDirectory(directory);
	}

}
