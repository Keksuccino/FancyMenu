package de.keksuccino.fancymenu.util.window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.MacosUtil;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class WindowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

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

	public static boolean isCustomWindowIconEnabled() {
		return FancyMenu.getOptions().showCustomWindowIcon.getValue();
	}

	@Nullable
	public static File getCustomWindowIcon16() {
		String path = FancyMenu.getOptions().customWindowIcon16.getValue();
		if (!path.replace(" ", "").isEmpty()) return new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
		return null;
	}

	@Nullable
	public static File getCustomWindowIcon32() {
		String path = FancyMenu.getOptions().customWindowIcon32.getValue();
		if (!path.replace(" ", "").isEmpty()) return new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
		return null;
	}

	@Nullable
	public static File getCustomWindowIconMacOS() {
		String path = FancyMenu.getOptions().customWindowIconMacOS.getValue();
		if (!path.replace(" ", "").isEmpty()) return new File(ScreenCustomization.getAbsoluteGameDirectoryPath(path));
		return null;
	}

	public static boolean allCustomWindowIconsSetAndFound() {
		File i16 = getCustomWindowIcon16();
		File i32 = getCustomWindowIcon32();
		File iMacOS = getCustomWindowIconMacOS();
		return ((i16 != null) && (i32 != null) && (iMacOS != null) && i16.isFile() && i32.isFile() && iMacOS.isFile());
	}

	public static void updateCustomWindowIcon() {
		if (Minecraft.ON_OSX) {
			updateCustomWindowIconMacOS();
		} else {
			updateCustomWindowIconWindowsLinux();
		}
	}

	private static void updateCustomWindowIconMacOS() {
		File iMacOS = getCustomWindowIconMacOS();
		if (isCustomWindowIconEnabled() && (iMacOS != null)) {
			try {
				if (!iMacOS.isFile()) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! 16x16 icon or 32x32 icon not found!");
					return;
				}
				MacosUtil.loadIcon(IoSupplier.create(iMacOS.toPath()));
			} catch (Exception ex) {
				LOGGER.error("[FANCYMENU] Failed to set custom window icon!");
				ex.printStackTrace();
			}
		}
	}

	private static void updateCustomWindowIconWindowsLinux() {
		File i16 = getCustomWindowIcon16();
		File i32 = getCustomWindowIcon32();
		if (isCustomWindowIconEnabled() && (i16 != null) && (i32 != null)) {
			try {
				if (!i16.exists() || !i32.exists()) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! 16x16 icon or 32x32 icon not found!");
					return;
				}
				BufferedImage i16buff = ImageIO.read(i16);
				if ((i16buff.getHeight() != 16) || (i16buff.getWidth() != 16)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! 16x16 icon has wrong resolution! Has To be exactly 16x16 pixels!");
					return;
				}
				BufferedImage i32buff = ImageIO.read(i32);
				if ((i32buff.getHeight() != 32) || (i32buff.getWidth() != 32)) {
					LOGGER.error("[FANCYMENU] Unable to set custom window icons! 32x32 icon has wrong resolution! Has To be exactly 32x32 pixels!");
					return;
				}
				Minecraft.getInstance().getWindow().setIcon(IoSupplier.create(i16.toPath()), IoSupplier.create(i32.toPath()));
				LOGGER.info("[FANCYMENU] Custom window icon successfully updated!");
			} catch (Exception e) {
				LOGGER.error("[FANCYMENU] Failed to set custom window icon!");
				e.printStackTrace();
			}
		}
	}

	public static void resetWindowIcon() {
		try {
			if (Minecraft.ON_OSX) {
				MacosUtil.loadIcon(getVanillaWindowIconFile("icons", "minecraft.icns"));
			} else {
				Minecraft.getInstance().getWindow().setIcon(getVanillaWindowIconFile("icons", "icon_16x16.png"), getVanillaWindowIconFile("icons", "icon_32x32.png"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static IoSupplier<InputStream> getVanillaWindowIconFile(String... $$0) throws IOException {
		IoSupplier<InputStream> $$1 = Minecraft.getInstance().getVanillaPackResources().getRootResource($$0);
		if ($$1 == null) {
			throw new FileNotFoundException(String.join("/", $$0));
		} else {
			return $$1;
		}
	}

	public static void updateWindowTitle() {
		Minecraft.getInstance().updateTitle();
	}

	@Nullable
	public static String getCustomWindowTitle() {
		String windowTitle = FancyMenu.getOptions().customWindowTitle.getValue();
		if (windowTitle.isEmpty()) {
			windowTitle = null;
		}
		return windowTitle;
	}

}
