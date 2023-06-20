package de.keksuccino.fancymenu.util.window;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WindowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final File ICON_DIR = new File(FancyMenu.MOD_DIR, "/minecraftwindow/icons");

	private static String windowTitle = null;
	
	public static void init() {
		if (!ICON_DIR.exists()) {
			ICON_DIR.mkdirs();
		}
	}

	public static void handleForceFullscreen() {
		try {
			if (FancyMenu.getOptions().forceFullscreen.getValue()) {
				if (!Minecraft.getInstance().getWindow().isFullscreen()) {
					Minecraft.getInstance().getWindow().toggleFullScreen();
					LOGGER.info("[FANCYMENU] Forced window to fullscreen!");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void updateWindowIcon() {
		if (FancyMenu.getOptions().showCustomWindowIcon.getValue()) {
			try {
				File i16 = new File(ICON_DIR.getPath() + "/icon16x16.png");
				File i32 = new File(ICON_DIR.getPath() + "/icon32x32.png");
				if (!i16.exists() || !i32.exists()) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons: 'icon16x16.png' or 'icon32x32.png' missing!");
					return;
				}
				//Yes, I need to do this to get the image size.
				BufferedImage i16buff = ImageIO.read(i16);
				if ((i16buff.getHeight() != 16) || (i16buff.getWidth() != 16)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons: 'icon16x16.png' not 16x16!");
					return;
				}
				BufferedImage i32buff = ImageIO.read(i32);
				if ((i32buff.getHeight() != 32) || (i32buff.getWidth() != 32)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons: 'icon32x32.png' not 32x32!");
					return;
				}
				
				Minecraft.getInstance().getWindow().setIcon(IoSupplier.create(i16.toPath()), IoSupplier.create(i32.toPath()));
				LOGGER.info("[FANCYMENU] Custom Minecraft window icon successfully loaded!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateWindowTitle() {
		String s = FancyMenu.getOptions().customWindowTitle.getValue();
		if (!s.isEmpty()) {
			windowTitle = s;
			setWindowTitle();
		} else {
			windowTitle = null;
		}
	}
	
	private static void setWindowTitle() {
		if (windowTitle != null) {
			Minecraft.getInstance().getWindow().setTitle(windowTitle);
		}
	}
	
	public static String getCustomWindowTitle() {
		return windowTitle;
	}

}
