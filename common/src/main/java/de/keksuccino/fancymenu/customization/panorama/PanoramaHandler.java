package de.keksuccino.fancymenu.customization.panorama;

import java.io.File;
import java.util.*;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PanoramaHandler {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final PanoramaRendererRegistry<LocalTexturePanoramaRenderer> PANORAMAS = new PanoramaRendererRegistry<>((renderer, throwable) -> LOGGER.error("[FANCYMENU] Failed to close cubic panorama renderer: " + renderer.getName(), throwable));
	public static final File PANORAMA_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "panoramas"));

	public static void init() {
		updatePanoramas();
		EventHandler.INSTANCE.registerListenersOf(new PanoramaHandler());
	}

	/**
	 * Rebuilds and replaces the complete owned renderer snapshot. This must stay on Minecraft's client/render thread:
	 * renderer construction and detached-renderer cleanup allocate and delete GPU resources directly.
	 */
	public static void updatePanoramas() {
		List<PanoramaRendererRegistry.Registration<LocalTexturePanoramaRenderer>> updatedPanoramas = new ArrayList<>();
		boolean replacementPublished = false;
		try {
			File[] files = Objects.requireNonNullElse(PANORAMA_DIR.listFiles(), new File[0]);
			Arrays.sort(files, Comparator.comparing(File::getName).thenComparing(File::getAbsolutePath));
			for (File panorama : files) {
				if (panorama.isDirectory()) {
					File propertiesFile = new File(panorama, "properties.txt");
					if (!propertiesFile.isFile()) {
						propertiesFile = new File(panorama, "properties.txt.txt");
					}
					File imageDir = new File(panorama, "panorama");
					File overlayImageFile = new File(panorama, "overlay.png");
					if (!overlayImageFile.isFile()) overlayImageFile = null;
					if (propertiesFile.isFile() && imageDir.isDirectory()) {
						LocalTexturePanoramaRenderer renderer = LocalTexturePanoramaRenderer.build(propertiesFile, imageDir, overlayImageFile);
						if (renderer != null) {
							updatedPanoramas.add(new PanoramaRendererRegistry.Registration<>(renderer.getName(), renderer));
						} else {
							LOGGER.error(buildErrorMessage(panorama, false, false, false) + " (failed to build renderer - was NULL)");
						}
					} else {
						LOGGER.error(buildErrorMessage(panorama, true, propertiesFile.isFile(), imageDir.isDirectory()));
					}
				} else {
					LOGGER.error(buildErrorMessage(panorama, false, false, false) + " (not a directory)");
				}
			}
			PANORAMAS.replaceAll(updatedPanoramas);
			replacementPublished = true;
		} finally {
			if (!replacementPublished) {
				PANORAMAS.discardUnpublished(updatedPanoramas);
			}
		}
	}

	/** Releases all renderer-owned GPU resources before vanilla tears down its texture manager and render device. */
	public static void shutdown() {
		PANORAMAS.close();
		PanoramaRotationTicker.shutdownSharedScheduler();
	}

	private static String buildErrorMessage(@NotNull File panoramaDir, boolean addDetails, boolean propertiesFileFound, boolean imageDirFound) {
		String msg = "[FANCYMENU] Invalid panorama found: " + panoramaDir.getName();
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

	@Nullable
	public static LocalTexturePanoramaRenderer getPanorama(@NotNull String name) {
		return PANORAMAS.get(name);
	}

	@NotNull
	public static List<LocalTexturePanoramaRenderer> getPanoramas() {
		return PANORAMAS.values();
	}

	@NotNull
	public static List<String> getPanoramaNames() {
		return PANORAMAS.names();
	}

	public static boolean panoramaExists(@NotNull String name) {
		return PANORAMAS.contains(name);
	}

	@EventListener
	public void onMenuReload(ModReloadEvent e) {
		LOGGER.info("[FANCYMENU] Reloading panoramas..");
		updatePanoramas();
	}

}
