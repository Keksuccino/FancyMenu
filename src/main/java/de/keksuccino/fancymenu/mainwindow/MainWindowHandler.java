package de.keksuccino.fancymenu.mainwindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Display;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.DisplayMode;

public class MainWindowHandler {

	private static File icondir = new File("config/fancymenu/minecraftwindow/icons");

	private static int scale = 1;
	private static boolean isScaleSet = false;
	
	public static void init() {

		if (!icondir.exists()) {
			icondir.mkdirs();
		}

	}

	public static boolean handleForceFullscreen() {
		try {
			if (FancyMenu.config.getOrDefault("forcefullscreen", false)) {
				Display.setFullscreen(true);
				DisplayMode displaymode = Display.getDisplayMode();
				Minecraft.getMinecraft().displayWidth = Math.max(1, displaymode.getWidth());
				Minecraft.getMinecraft().displayHeight = Math.max(1, displaymode.getHeight());
				FancyMenu.LOGGER.info("[FANCYMENU] Forced window to fullscreen!");
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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

		        int[] i1 = i16buff.getRGB(0, 0, i16buff.getWidth(), i16buff.getHeight(), (int[])null, 0, i16buff.getWidth());
		        ByteBuffer i16bytebuffer = ByteBuffer.allocate(4 * i1.length);
		        for (int i : i1) {
		        	i16bytebuffer.putInt(i << 8 | i >> 24 & 255);
		        }
		        i16bytebuffer.flip();
		        
		        int[] i2 = i32buff.getRGB(0, 0, i32buff.getWidth(), i32buff.getHeight(), (int[])null, 0, i32buff.getWidth());
		        ByteBuffer i32bytebuffer = ByteBuffer.allocate(4 * i2.length);
		        for (int i : i2) {
		        	i32bytebuffer.putInt(i << 8 | i >> 24 & 255);
		        }
		        i32bytebuffer.flip();
		        
		        Display.setIcon(new ByteBuffer[] {i16bytebuffer, i32bytebuffer});
		        
				System.out.println("[FANCYMENU] Custom minecraft icon successfully loaded!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getCustomWindowTitle() {
		String s = FancyMenu.config.getOrDefault("customwindowtitle", "");
		if ((s != null) && (!s.equals(""))) {
			return s;
		}
		return "Minecraft 1.12.2";
	}
	
	/**
	 * Will return the correct window width <b>while in a GUI</b>.<br>
	 * <b>Returns 0 if no GUI is active!</b>
	 */
	public static int getWindowGuiWidth() {
		GuiScreen s = Minecraft.getMinecraft().currentScreen;
		if (s != null) {
			double mcScale = Minecraft.getMinecraft().gameSettings.guiScale;
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
		GuiScreen s = Minecraft.getMinecraft().currentScreen;
		if (s != null) {
			double mcScale = Minecraft.getMinecraft().gameSettings.guiScale;
			float baseUIScale = 1.0F;
			float sc = (float) (((double)baseUIScale) * (((double)baseUIScale) / mcScale));
			
			return (int) (s.height / sc);
		}
		return 0;
	}
	
	public static int getScaledWidth() {
		ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
		return res.getScaledWidth();
	}
	
	public static int getScaledHeight() {
		ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
		return res.getScaledHeight();
	}

	public static void setGuiScale(int scale) {
		MainWindowHandler.scale = scale;
		isScaleSet = true;
	}

	public static int getGuiScale() {
		return scale;
	}

	public static void resetGuiScale() {
		scale = 1;
		isScaleSet = false;
	}

	public static boolean isGuiScaleSet() {
		return isScaleSet;
	}

}
