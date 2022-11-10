package de.keksuccino.fancymenu.mainwindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MainWindowHandler {

	private static String windowtitle = null;
	//---
	private static File icondir = new File(FancyMenu.MOD_DIR, "/minecraftwindow/icons");
	
	public static void init() {
		if (!icondir.exists()) {
			icondir.mkdirs();
		}
	}

	public static void handleForceFullscreen() {
		try {
			if ((Minecraft.getInstance() != null) && (Minecraft.getInstance().getWindow() != null)) {
				if (FancyMenu.config.getOrDefault("forcefullscreen", false)) {
					if (!Minecraft.getInstance().getWindow().isFullscreen()) {
						Minecraft.getInstance().getWindow().toggleFullScreen();
						FancyMenu.LOGGER.info("[FANCYMENU] Forced window to fullscreen!");
					}
				}
			} else {
				//This should basically never happen, but just in case, you know
				FancyMenu.LOGGER.error("[FANCYMENU] Force fullscreen failed! Instance or window was NULL!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void updateWindowIcon() {
		if (FancyMenu.config.getOrDefault("customwindowicon", false)) {
			try {
				File i16 = new File(icondir.getPath() + "/icon16x16.png");
				File i32 = new File(icondir.getPath() + "/icon32x32.png");
				if (!i16.exists() || !i32.exists()) {
					System.out.println("## ERROR ## [FANCYMENU] Unable to set custom icons: 'icon16x16.png' or 'icon32x32.png' missing!");
					return;
				}
				//Yes, I need to do this to get the image size.
				BufferedImage i16buff = ImageIO.read(i16);
				if ((i16buff.getHeight() != 16) || (i16buff.getWidth() != 16)) {
					System.out.println("'## ERROR ## [FANCYMENU] Unable to set custom icons: 'icon16x16.png' not 16x16!");
					return;
				}
				BufferedImage i32buff = ImageIO.read(i32);
				if ((i32buff.getHeight() != 32) || (i32buff.getWidth() != 32)) {
					System.out.println("'## ERROR ## [FANCYMENU] Unable to set custom icons: 'icon32x32.png' not 32x32!");
					return;
				}
				InputStream icon16 = new FileInputStream(i16);
				InputStream icon32 = new FileInputStream(i32);
				
				Minecraft.getInstance().getWindow().setIcon(icon16, icon32);
				System.out.println("[FANCYMENU] Custom minecraft icon successfully loaded!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateWindowTitle() {
		String s = FancyMenu.config.getOrDefault("customwindowtitle", "");
		if ((s != null) && (!s.equals(""))) {
			windowtitle = s;
			setWindowTitle();
		} else {
			windowtitle = null;
		}
	}
	
	private static void setWindowTitle() {
		if (windowtitle != null) {
			Minecraft.getInstance().getWindow().setTitle(windowtitle);
		}
	}
	
	public static String getCustomWindowTitle() {
		return windowtitle;
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
