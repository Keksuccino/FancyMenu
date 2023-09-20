package de.keksuccino.fancymenu.customization.slideshow;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlideshowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final File SLIDESHOW_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/slideshows"));

	private static final Map<String, ExternalTextureSlideshowRenderer> SLIDESHOWS = new HashMap<>();
	
	public static void init() {
		updateSlideshows();
		EventHandler.INSTANCE.registerListenersOf(new SlideshowHandler());
	}
	
	public static void updateSlideshows() {
		SLIDESHOWS.clear();
		File[] files = SLIDESHOW_DIR.listFiles();
		if (files == null) return;
		for (File slideshow : files) {
			if (slideshow.isDirectory()) {
				File propertiesFile = new File(slideshow.getPath() + "/properties.txt");
				if (!propertiesFile.exists()) {
					new File(slideshow.getPath() + "/properties.txt.txt");
				}
				File imageDir = new File(slideshow.getPath() + "/images");
				if (propertiesFile.exists() && imageDir.exists()) {
					ExternalTextureSlideshowRenderer render = new ExternalTextureSlideshowRenderer(slideshow.getPath());
					String name = render.getName();
					if (name != null) {
						render.prepareSlideshow();
						SLIDESHOWS.put(name, render);
					} else {
						LOGGER.error(buildErrorMessage(slideshow, false, false, false) + " (name is empty/NULL)");
					}
				} else {
					LOGGER.error(buildErrorMessage(slideshow, true, propertiesFile.exists(), imageDir.exists()));
				}
			} else {
				LOGGER.error(buildErrorMessage(slideshow, false, false, false) + " (not a directory)");
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

	@Nullable
	public static ExternalTextureSlideshowRenderer getSlideshow(@NotNull String identifier) {
		return SLIDESHOWS.get(identifier);
	}

	@NotNull
	public static List<ExternalTextureSlideshowRenderer> getSlideshows() {
		return new ArrayList<>(SLIDESHOWS.values());
	}

	@NotNull
	public static List<String> getSlideshowNames() {
		return new ArrayList<>(SLIDESHOWS.keySet());
	}
	
	public static boolean slideshowExists(@NotNull String identifier) {
		return SLIDESHOWS.containsKey(identifier);
	}
	
	@EventListener
	public void onMenuReload(ModReloadEvent e) {
		LOGGER.info("[FANCYMENU] Reloading slideshows..");
		updateSlideshows();
	}

}
