package de.keksuccino.fancymenu.customization.slideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlideshowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<String, ExternalTextureSlideshowRenderer> SLIDESHOWS = new HashMap<>();
	
	public static void init() {
		updateSlideshows();
		EventHandler.INSTANCE.registerListenersOf(new SlideshowHandler());
	}
	
	public static void updateSlideshows() {
		File f = FancyMenu.getSlideshowDirectory();
		SLIDESHOWS.clear();
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory()) {
				File f3 = new File(f2.getPath() + "/properties.txt");
				if (!f3.exists()) {
					new File(f2.getPath() + "/properties.txt.txt");
				}
				File f4 = new File(f2.getPath() + "/images");
				if (f3.exists() && f4.exists()) {
					ExternalTextureSlideshowRenderer render = new ExternalTextureSlideshowRenderer(f2.getPath());
					String name = render.getName();
					if (name != null) {
						render.prepareSlideshow();
						SLIDESHOWS.put(name, render);
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
		String msg = "[FANCYMENU] Invalid slideshow found: " + f.getName();
		if (addDetails) {
			String details = "";
			if (!propertiesFileFound) {
				details += "Missing \"properties.txt\" file";
			}
			if (!details.isEmpty()) {
				details += "; ";
			}
			if (!imageDirFound) {
				details += "Missing \"images\" directory";
			}
			if (!details.isEmpty()) {
				msg += " (" + details + ")";
			}
		}
		return msg;
	}
	
	public static ExternalTextureSlideshowRenderer getSlideshow(String name) {
		return SLIDESHOWS.get(name);
	}
	
	public static List<ExternalTextureSlideshowRenderer> getSlideshows() {
		List<ExternalTextureSlideshowRenderer> l = new ArrayList<ExternalTextureSlideshowRenderer>();
		l.addAll(SLIDESHOWS.values());
		return l;
	}
	
	public static List<String> getSlideshowNames() {
		List<String> l = new ArrayList<String>();
		l.addAll(SLIDESHOWS.keySet());
		return l;
	}
	
	public static boolean slideshowExists(String name) {
		return SLIDESHOWS.containsKey(name);
	}
	
	@EventListener
	public void onMenuReload(ModReloadEvent e) {
		LOGGER.info("[FANCYMENU] Reloading slideshows..");
		updateSlideshows();
	}

}
