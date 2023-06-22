package de.keksuccino.fancymenu.util.window;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class WindowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final File ICON_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/minecraftwindow/icons"));

	private static String windowTitle = null;
	private static boolean initialized = false;

	public static void init() {
		if (!initialized) {
			updateWindowIcon();
			updateWindowTitleInternal();
		}
		initialized = true;
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
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! Icon image 'icon16x16.png' or 'icon32x32.png' missing!");
					return;
				}
				BufferedImage i16buff = ImageIO.read(i16);
				if ((i16buff.getHeight() != 16) || (i16buff.getWidth() != 16)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! Icon image 'icon16x16.png' has wrong resolution! Has To be exactly 16x16 pixels!");
					return;
				}
				BufferedImage i32buff = ImageIO.read(i32);
				if ((i32buff.getHeight() != 32) || (i32buff.getWidth() != 32)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! Icon image 'icon32x32.png' has wrong resolution! Has To be exactly 32x32 pixels!");
					return;
				}
				Minecraft.getInstance().getWindow().setIcon(IoSupplier.create(i16.toPath()), IoSupplier.create(i32.toPath()));
				LOGGER.info("[FANCYMENU] Custom window icon successfully updated!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void updateWindowTitle() {
		updateWindowTitleInternal();
		Minecraft.getInstance().updateTitle();
	}

	private static void updateWindowTitleInternal() {
		windowTitle = FancyMenu.getOptions().customWindowTitle.getValue();
		if (windowTitle.isEmpty()) {
			windowTitle = null;
		}
	}

	@Nullable
	public static String getCustomWindowTitle() {
		return windowTitle;
	}

}
