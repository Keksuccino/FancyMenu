package de.keksuccino.core.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CurrentScreenHandler {
	
	private static MatrixStack currentStack;
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new CurrentScreenHandler());
	}
	
	public static Screen getScreen() {
		return Minecraft.getInstance().currentScreen;
	}
	
	/**
	 * Returns the {@link MatrixStack} for the current game tick or NULL if there is no stack.<br><br>
	 * 
	 * <b>IF NO SCREEN IS BEING RENDERED ATM, THIS WILL RETURN THE LAST STACK USED TO RENDER A SCREEN!</b>
	 */
	public static MatrixStack getMatrixStack() {
		return currentStack;
	}
	
	public static int getWidth() {
		if (getScreen() != null) {
			return getScreen().field_230708_k_;
		}
		return 0;
	}
	
	public static void setWidth(int width) {
		if (getScreen() != null) {
			getScreen().field_230708_k_ = width;
		}
	}
	
	public static int getHeight() {
		if (getScreen() != null) {
			return getScreen().field_230709_l_;
		}
		return 0;
	}
	
	public static void setHeight(int height) {
		if (getScreen() != null) {
			getScreen().field_230709_l_ = height;
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre e) {
		currentStack = e.getMatrixStack();
	}

}
