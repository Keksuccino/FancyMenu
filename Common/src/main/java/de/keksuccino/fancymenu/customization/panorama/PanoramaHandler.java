package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PanoramaHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final File PANORAMA_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/panoramas"));

	private static final Map<String, ExternalTexturePanoramaRenderer> PANORAMAS = new HashMap<>();
	
	public static void init() {
		updatePanoramas();
		EventHandler.INSTANCE.registerListenersOf(new PanoramaHandler());
	}
	
	public static void updatePanoramas() {
		PANORAMAS.clear();
		File[] files = PANORAMA_DIR.listFiles();
		if (files == null) return;
		for (File panorama : files) {
			if (panorama.isDirectory()) {
				File propertiesFile = new File(panorama.getPath() + "/properties.txt");
				if (!propertiesFile.exists()) {
					propertiesFile = new File(panorama.getPath() + "/properties.txt.txt");
				}
				File imageDir = new File(panorama.getPath() + "/panorama");
				if (propertiesFile.exists() && imageDir.exists()) {
					ExternalTexturePanoramaRenderer render = new ExternalTexturePanoramaRenderer(panorama.getPath());
					String name = render.getName();
					if (name != null) {
						render.preparePanorama();
						PANORAMAS.put(name, render);
					} else {
						LOGGER.error(buildErrorMessage(panorama, false, false, false) + " (name is empty/NULL)");
					}
				} else {
					LOGGER.error(buildErrorMessage(panorama, true, propertiesFile.exists(), imageDir.exists()));
				}
			} else {
				LOGGER.error(buildErrorMessage(panorama, false, false, false) + " (not a directory)");
			}
		}
	}

	private static String buildErrorMessage(File f, boolean addDetails, boolean propertiesFileFound, boolean imageDirFound) {
		String msg = "[FANCYMENU] Invalid panorama found: " + f.getName();
		if (addDetails) {
			String details = "";
			if (!propertiesFileFound) {
				details += "Missing \"properties.txt\" file";
			}
			if (!details.isEmpty()) {
				details += "; ";
			}
			if (!imageDirFound) {
				details += "Missing \"panorama\" directory for images";
			}
			if (!details.isEmpty()) {
				msg += " (" + details + ")";
			}
		}
		return msg;
	}
	
	public static ExternalTexturePanoramaRenderer getPanorama(String name) {
		return PANORAMAS.get(name);
	}
	
	public static List<ExternalTexturePanoramaRenderer> getPanoramas() {
		return new ArrayList<>(PANORAMAS.values());
	}
	
	public static List<String> getPanoramaNames() {
		return new ArrayList<>(PANORAMAS.keySet());
	}
	
	public static boolean panoramaExists(String name) {
		return PANORAMAS.containsKey(name);
	}
	
	@EventListener
	public void onMenuReload(ModReloadEvent e) {
		LOGGER.info("[FANCYMENU] Reloading panoramas..");
		updatePanoramas();
	}

}
