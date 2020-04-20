package de.keksuccino.core.input;

import java.lang.reflect.Field;

import de.keksuccino.core.reflection.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;

public class MouseInput {
	
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
		return (getActiveMouseButton() == 0);
	}
	
	public static boolean isRightMouseDown() {
		return (getActiveMouseButton() == 1);
	}
	
	public static int getMouseX() {
		Minecraft.getInstance().mouseHelper.getMouseX();
		return (int)(Minecraft.getInstance().mouseHelper.getMouseX() * (double)Minecraft.getInstance().func_228018_at_().getScaledWidth() / (double)Minecraft.getInstance().func_228018_at_().getWidth());
	}
	
	public static int getMouseY() {
		return (int)(Minecraft.getInstance().mouseHelper.getMouseY() * (double)Minecraft.getInstance().func_228018_at_().getScaledHeight() / (double)Minecraft.getInstance().func_228018_at_().getHeight());
	}

}
