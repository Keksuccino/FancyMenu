package de.keksuccino.core.input;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import de.keksuccino.core.resources.ResourceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class MouseInput {
	
	private static Cursor VRESIZE_CURSOR;
	private static Cursor HRESIZE_CURSOR;
	
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			VRESIZE_CURSOR = loadCursor(new ResourceLocation("keksuccino", "cursor/vresize.png"), 32, 32, 16, 16);
			HRESIZE_CURSOR = loadCursor(new ResourceLocation("keksuccino", "cursor/hresize.png"), 32, 32, 16, 16);
			init = true;
		}
	}
	
	public static boolean isLeftMouseDown() {
		return Mouse.isButtonDown(0);
	}
	
	public static boolean isRightMouseDown() {
		return Mouse.isButtonDown(1);
	}
	
	public static int getMouseX() {
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int i1 = scaledresolution.getScaledWidth();
        return Mouse.getX() * i1 / Minecraft.getMinecraft().displayWidth;
	}
	
	public static int getMouseY() {
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int j1 = scaledresolution.getScaledHeight();
        return j1 - Mouse.getY() * j1 / Minecraft.getMinecraft().displayHeight - 1;
	}
	
	public static void setCursor(CursorType cursor) {
		try {
			if (cursor == null) {
				resetCursor();
			}
			if (cursor == CursorType.HRESIZE) {
				Mouse.setNativeCursor(HRESIZE_CURSOR);
			}
			if (cursor == CursorType.VRESIZE) {
				Mouse.setNativeCursor(VRESIZE_CURSOR);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void resetCursor() {
		try {
			Mouse.setNativeCursor(null);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}
	
	private static Cursor loadCursor(ResourceLocation r, int width, int height, int xHotspot, int yHotspot) {
		try {
			BufferedImage i = ResourceUtils.getImageResourceAsStream(r);
			if (i.getType() != BufferedImage.TYPE_INT_ARGB) {
			    BufferedImage tmp = new BufferedImage(i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_ARGB);
			    tmp.getGraphics().drawImage(i, 0, 0, null);
			    i = tmp;
			}
			int[] srcPixels = ((DataBufferInt)i.getRaster().getDataBuffer()).getData();
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(srcPixels.length * 4);
	        byteBuffer.order(ByteOrder.nativeOrder());
	        IntBuffer intBuffer = byteBuffer.asIntBuffer();
	        intBuffer.put(srcPixels);
	        intBuffer.position(0);

	        return new Cursor(width, height, xHotspot, yHotspot, 1, intBuffer, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static enum CursorType {
		VRESIZE,
		HRESIZE;
	}

}
