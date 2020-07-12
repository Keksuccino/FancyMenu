package de.keksuccino.core.input;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.keksuccino.core.reflection.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MouseInput {
	
	private static boolean leftClicked = false;
	private static boolean rightClicked  = false;
	private static Map<String, Boolean> vanillainput = new HashMap<String, Boolean>();
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new MouseInput());
	}
	
	public static int getActiveMouseButton() {
		int b = -1;
		Field f = ReflectionHelper.findField(MouseHelper.class, "field_198042_g");
		try {
			b = (int) f.get(Minecraft.getInstance().mouseHelper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}
	
	public static boolean isLeftMouseDown() {
		return leftClicked;
	}

	public static boolean isRightMouseDown() {
		return rightClicked;
	}
	
	public static int getMouseX() {
		return (int)(Minecraft.getInstance().mouseHelper.getMouseX() * (double)Minecraft.getInstance().getMainWindow().getScaledWidth() / (double)Minecraft.getInstance().getMainWindow().getWidth());
	}
	
	public static int getMouseY() {
		return (int)(Minecraft.getInstance().mouseHelper.getMouseY() * (double)Minecraft.getInstance().getMainWindow().getScaledHeight() / (double)Minecraft.getInstance().getMainWindow().getHeight());
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
	
	@SubscribeEvent
	public void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre e) {
		int i = e.getButton();
		if (i == 0) {
			leftClicked = true;
		}
		if (i == 1) {
			rightClicked = true;
		}
		
		if (isVanillaInputBlocked()) {
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onMouseReleased(GuiScreenEvent.MouseReleasedEvent.Pre e) {
		int i = e.getButton();
		if (i == 0) {
			leftClicked = false;
		}
		if (i == 1) {
			rightClicked = false;
		}
	}
	
	@SubscribeEvent
	public void onScreenInit(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Reset values on screen init
		leftClicked = false;
		rightClicked = false;
	}

}
