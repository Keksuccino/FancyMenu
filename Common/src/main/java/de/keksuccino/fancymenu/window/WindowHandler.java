package de.keksuccino.fancymenu.window;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
			if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().getWindow() != null)) {
				if (FancyMenu.getConfig().getOrDefault("forcefullscreen", false)) {
					if (!Minecraft.getInstance().getWindow().isFullscreen()) {
						Minecraft.getInstance().getWindow().toggleFullScreen();
						LOGGER.info("[FANCYMENU] Forced window to fullscreen!");
					}
				}
			} else {
				//This should basically never happen, but just in case, you know
				LOGGER.error("[FANCYMENU] Failed to force fullscreen! Instance or window was NULL!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void updateWindowIcon() {
		if (FancyMenu.getConfig().getOrDefault("customwindowicon", false)) {
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
		String s = FancyMenu.getConfig().getOrDefault("customwindowtitle", "");
		if ((s != null) && (!s.equals(""))) {
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

	/**
	 * Will return the correct window width <b>while in a GUI</b>.<br>
	 * <b>Returns 0 if no GUI is active!</b>
	 */
	public static int getWindowGuiWidth() {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			double mcScale = Minecraft.getInstance().getWindow().calculateScale((int) Minecraft.getInstance().getWindow().getGuiScale(), Minecraft.getInstance().options.forceUnicodeFont().get());
			float baseUIScale = 1.0F;
			float sc = (float) (((double)baseUIScale) * (((double)baseUIScale) / mcScale));
			
			return (int) (s.width / sc);
		}
		return 0;
	}

	/**
	 * Will return the correct window height <b>while in a GUI</b>.<br>
	 * <b>Returns 0 if no GUI is active!</b>
	 */
	public static int getWindowGuiHeight() {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			double mcScale = Minecraft.getInstance().getWindow().calculateScale((int) Minecraft.getInstance().getWindow().getGuiScale(), Minecraft.getInstance().options.forceUnicodeFont().get());
			float baseUIScale = 1.0F;
			float sc = (float) (((double)baseUIScale) * (((double)baseUIScale) / mcScale));
			
			return (int) (s.height / sc);
		}
		return 0;
	}

}
