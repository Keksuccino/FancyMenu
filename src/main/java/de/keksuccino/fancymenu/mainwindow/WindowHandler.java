package de.keksuccino.fancymenu.mainwindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class WindowHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final File ICON_DIR = new File(FancyMenu.MOD_DIR, "/minecraftwindow/icons");

	private static String windowTitle = null;

	public static void handleForceFullscreen() {
		try {
			FancyMenu.initConfig();
			if (FancyMenu.config.getOrDefault("forcefullscreen", false)) {
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
		FancyMenu.initConfig();
		if (FancyMenu.config.getOrDefault("customwindowicon", false)) {
			try {
				File i16 = new File(ICON_DIR.getPath() + "/icon16x16.png");
				File i32 = new File(ICON_DIR.getPath() + "/icon32x32.png");
				if (!i16.exists() || !i32.exists()) {
					LOGGER.error("[FANCYMENU] Unable to set custom icons: 'icon16x16.png' or 'icon32x32.png' missing!");
					return;
				}
				//Yes, I need to do this to get the image size.
				BufferedImage i16buff = ImageIO.read(i16);
				if ((i16buff.getHeight() != 16) || (i16buff.getWidth() != 16)) {
					LOGGER.error("[FANCYMENU] Unable to set custom icons: 'icon16x16.png' not 16x16!");
					return;
				}
				BufferedImage i32buff = ImageIO.read(i32);
				if ((i32buff.getHeight() != 32) || (i32buff.getWidth() != 32)) {
					LOGGER.error("[FANCYMENU] Unable to set custom icons: 'icon32x32.png' not 32x32!");
					return;
				}
				setIcon(IoSupplier.create(i16.toPath()), IoSupplier.create(i32.toPath()));
				LOGGER.info("[FANCYMENU] Custom minecraft icon successfully loaded!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void setIcon(IoSupplier<InputStream> p_250818_, IoSupplier<InputStream> p_249199_) {
		RenderSystem.assertInInitPhase();
		try (MemoryStack memorystack = MemoryStack.stackPush()) {
			IntBuffer intbuffer = memorystack.mallocInt(1);
			IntBuffer intbuffer1 = memorystack.mallocInt(1);
			IntBuffer intbuffer2 = memorystack.mallocInt(1);
			GLFWImage.Buffer buffer = GLFWImage.malloc(2, memorystack);
			ByteBuffer bytebuffer = readIconPixels(p_250818_, intbuffer, intbuffer1, intbuffer2);
			if (bytebuffer == null) {
				throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
			}
			buffer.position(0);
			buffer.width(intbuffer.get(0));
			buffer.height(intbuffer1.get(0));
			buffer.pixels(bytebuffer);
			ByteBuffer bytebuffer1 = readIconPixels(p_249199_, intbuffer, intbuffer1, intbuffer2);
			if (bytebuffer1 == null) {
				STBImage.stbi_image_free(bytebuffer);
				throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
			}
			buffer.position(1);
			buffer.width(intbuffer.get(0));
			buffer.height(intbuffer1.get(0));
			buffer.pixels(bytebuffer1);
			buffer.position(0);
			GLFW.glfwSetWindowIcon(Minecraft.getInstance().getWindow().getWindow(), buffer);
			STBImage.stbi_image_free(bytebuffer);
			STBImage.stbi_image_free(bytebuffer1);
		} catch (IOException ioexception) {
			LogManager.getLogger().error("Couldn't set icon", ioexception);
		}
	}

	@Nullable
	private static ByteBuffer readIconPixels(IoSupplier<InputStream> p_249586_, IntBuffer p_249069_, IntBuffer p_249100_, IntBuffer p_249695_) throws IOException {
		RenderSystem.assertInInitPhase();
		ByteBuffer bytebuffer = null;
		try (InputStream inputstream = p_249586_.get()) {
			bytebuffer = TextureUtil.readResource(inputstream);
			bytebuffer.rewind();
			return STBImage.stbi_load_from_memory(bytebuffer, p_249069_, p_249100_, p_249695_, 0);
		} finally {
			if (bytebuffer != null) {
				MemoryUtil.memFree(bytebuffer);
			}
		}
	}

	public static void updateWindowTitle() {
		readCustomWindowTitleFromConfig();
		Minecraft.getInstance().updateTitle();
	}

	public static void readCustomWindowTitleFromConfig() {
		FancyMenu.initConfig();
		windowTitle = FancyMenu.config.getOrDefault("customwindowtitle", "");
		if ((windowTitle != null) && windowTitle.isEmpty()) {
			windowTitle = null;
		}
	}

	@org.jetbrains.annotations.Nullable
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

