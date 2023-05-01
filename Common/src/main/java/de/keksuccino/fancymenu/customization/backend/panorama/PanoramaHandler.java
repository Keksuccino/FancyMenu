package de.keksuccino.fancymenu.customization.backend.panorama;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PanoramaHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<String, ExternalTexturePanoramaRenderer> PANORAMAS = new HashMap<>();
	
	public static void init() {
		updatePanoramas();
		EventHandler.INSTANCE.registerListenersOf(new PanoramaHandler());
	}
	
	public static void updatePanoramas() {
		File f = FancyMenu.getPanoramaDirectory();
		PANORAMAS.clear();
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory()) {
				File f3 = new File(f2.getPath() + "/properties.txt");
				if (!f3.exists()) {
					f3 = new File(f2.getPath() + "/properties.txt.txt");
				}
				File f4 = new File(f2.getPath() + "/panorama");
				if (f3.exists() && f4.exists()) {
					ExternalTexturePanoramaRenderer render = new ExternalTexturePanoramaRenderer(f2.getPath());
					String name = render.getName();
					if (name != null) {
						render.preparePanorama();
						PANORAMAS.put(name, render);
					} else {
						LOGGER.error(buildErrorMessage(f2, false, false, false) + " (name is empty/NULL)");
					}
				} else {
					LOGGER.error(buildErrorMessage(f2, true, f3.exists(), f4.exists()));
				}
			} else {
				LOGGER.error(buildErrorMessage(f2, false, false, false) + " (not a directory)");
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
	public void onMenuReload(MenuReloadEvent e) {
		LOGGER.info("[FANCYMENU] Reloading panoramas..");
		updatePanoramas();
	}

}
