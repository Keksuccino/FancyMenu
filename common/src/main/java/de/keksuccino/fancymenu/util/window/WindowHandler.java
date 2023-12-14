package de.keksuccino.fancymenu.util.window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import ca.weblite.objc.Client;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
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
		if (!path.replace(" ", "").isEmpty()) return new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path));
		return null;
	}

	@Nullable
	public static File getCustomWindowIcon32() {
		String path = FancyMenu.getOptions().customWindowIcon32.getValue();
		if (!path.replace(" ", "").isEmpty()) return new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path));
		return null;
	}

	@Nullable
	public static File getCustomWindowIconMacOS() {
		String path = FancyMenu.getOptions().customWindowIconMacOS.getValue();
		if (!path.replace(" ", "").isEmpty()) return new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path));
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
				loadMacOsIcon(InputStreamSupplier.create(iMacOS.toPath()));
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
				Minecraft.getInstance().getWindow().setIcon(InputStreamSupplier.create(i16.toPath()).get(), InputStreamSupplier.create(i32.toPath()).get());
				LOGGER.info("[FANCYMENU] Custom window icon successfully updated!");
			} catch (Exception e) {
				LOGGER.error("[FANCYMENU] Failed to set custom window icon!");
				e.printStackTrace();
			}
		}
	}

	public static void resetWindowIcon() {
		try {
			setVanillaIcons();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void setVanillaIcons() {
		if (Minecraft.ON_OSX) return;
		try {
			InputStream $$9 = Minecraft.getInstance().getClientPackSource()
					.getVanillaPack()
					.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_16x16.png"));
			InputStream $$10 = Minecraft.getInstance().getClientPackSource()
					.getVanillaPack()
					.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_32x32.png"));
			Minecraft.getInstance().getWindow().setIcon($$9, $$10);
		} catch (IOException ex) {
			LOGGER.error("Couldn't set icon", ex);
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

	@SuppressWarnings("all")
	public static void loadMacOsIcon(InputStreamSupplier<InputStream> inputStreamSupplier) throws IOException {
		try (InputStream in = inputStreamSupplier.get()) {
			String $$2 = Base64.getEncoder().encodeToString(in.readAllBytes());
			Client $$3 = Client.getInstance();
			Object $$4 = $$3.sendProxy("NSData", "alloc", new Object[0]).send("initWithBase64Encoding:", new Object[]{$$2});
			Object $$5 = $$3.sendProxy("NSImage", "alloc", new Object[0]).send("initWithData:", new Object[]{$$4});
			$$3.sendProxy("NSApplication", "sharedApplication", new Object[0]).send("setApplicationIconImage:", new Object[]{$$5});
		}
	}

	@FunctionalInterface
	private interface InputStreamSupplier<T> {

		static InputStreamSupplier<InputStream> create(Path $$0) {
			return () -> Files.newInputStream($$0);
		}

		T get() throws IOException;

	}

}
