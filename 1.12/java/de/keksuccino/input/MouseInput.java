package de.keksuccino.input;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class MouseInput {
	
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

}
