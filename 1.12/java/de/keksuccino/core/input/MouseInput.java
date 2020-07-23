package de.keksuccino.core.input;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import de.keksuccino.core.resources.ResourceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MouseInput {
	
	private static Cursor VRESIZE_CURSOR;
	private static Cursor HRESIZE_CURSOR;
	
	private static boolean leftClicked = false;
	private static boolean rightClicked  = false;
	private static Map<String, Boolean> vanillainput = new HashMap<String, Boolean>();
	
	private static List<Consumer<MouseData>> listeners = new ArrayList<Consumer<MouseData>>();
	
	public static void init() {
		VRESIZE_CURSOR = loadCursor(new ResourceLocation("keksuccino", "cursor/vresize.png"), 32, 32, 16, 16);
		HRESIZE_CURSOR = loadCursor(new ResourceLocation("keksuccino", "cursor/hresize.png"), 32, 32, 16, 16);
		
		MinecraftForge.EVENT_BUS.register(new MouseInput());
	}
	
	public static boolean isLeftMouseDown() {
		return leftClicked;
	}

	public static boolean isRightMouseDown() {
		return rightClicked;
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
	
	public static void blockVanillaInput(String category) {
		vanillainput.put(category, true);
	}
	
	public static void unblockVanillaInput(String category) {
		vanillainput.put(category, false);
	}
	
	public static boolean isVanillaInputBlocked() {
		return vanillainput.containsValue(true);
	}
	
	public static void registerMouseListener(Consumer<MouseData> c) {
		listeners.add(c);
	}
	
	@SubscribeEvent
	public void onMouseClicked(GuiScreenEvent.MouseInputEvent.Pre e) {
		
		for (Consumer<MouseData> c : listeners) {
			c.accept(new MouseData(getMouseX(), getMouseY(), Mouse.getEventDX(), Mouse.getEventDY(), Mouse.getEventDWheel()));
		}
		
		int i = Mouse.getEventButton();
		if ((i == 0) && Mouse.getEventButtonState()) {
			leftClicked = true;
		}
		if ((i == 1) && Mouse.getEventButtonState()) {
			rightClicked = true;
		}
		if ((i == 0) && !Mouse.getEventButtonState()) {
			leftClicked = false;
		}
		if ((i == 1) && !Mouse.getEventButtonState()) {
			rightClicked = false;
		}
		
		if (isVanillaInputBlocked()) {
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onScreenInit(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Reset values on screen init
		leftClicked = false;
		rightClicked = false;
	}
	
	public static class MouseData {
		public int deltaX;
		public int deltaY;
		public int deltaZ;
		
		public int mouseX;
		public int mouseY;
		
		public MouseData(int mouseX, int mouseY, int deltaX, int deltaY, int deltaZ) {
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			this.deltaZ = deltaZ;
			this.mouseX = mouseX;
			this.mouseY = mouseY;
		}
	}

}
