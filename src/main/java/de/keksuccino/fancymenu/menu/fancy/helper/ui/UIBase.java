package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import java.awt.Color;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class UIBase extends DrawableHelper {

	protected static Color idleButtonColor;
	protected static Color hoveredButtonColor;
	protected static Color idleButtonBorderColor;
	protected static Color hoveredButtonBorderColor;
	protected static float buttonBorderWidth = 1.0F;
	
	protected static float baseUIScale = 1.0F;
	
	//Why? Because macOS + fabric is crap and doesn't let me set the button colors on mod init :)
	protected static void initButtonColors() {
		if (idleButtonColor == null) {

			idleButtonColor = new Color(71, 71, 71);
			hoveredButtonColor = new Color(83, 156, 212);
			idleButtonBorderColor = new Color(209, 194, 209);
			hoveredButtonBorderColor = new Color(227, 211, 227);
			
		}
	}
	
	public static void colorizeButton(AdvancedButton button) {
		initButtonColors();
		button.setBackgroundColor(idleButtonColor, hoveredButtonColor, idleButtonBorderColor, hoveredButtonBorderColor, buttonBorderWidth);
	}
	
	public static Color getButtonIdleColor() {
		initButtonColors();
		return idleButtonColor;
	}
	
	public static Color getButtonBorderIdleColor() {
		initButtonColors();
		return idleButtonBorderColor;
	}
	
	public static Color getButtonHoverColor() {
		initButtonColors();
		return hoveredButtonColor;
	}
	
	public static Color getButtonBorderHoverColor() {
		initButtonColors();
		return hoveredButtonBorderColor;
	}
	
	public static float getUIScale() {

		float uiScale = FancyMenu.config.getOrDefault("uiscale", 1.0F);
		double mcScale = MinecraftClient.getInstance().getWindow().getScaleFactor();

		return (float) ((((double)baseUIScale) * (((double)baseUIScale) / mcScale)) * uiScale);
		
	}
	
	public static void openScaledContextMenuAt(FMContextMenu menu, int x, int y) {
		Screen s = MinecraftClient.getInstance().currentScreen;
		if (s != null) {
			menu.openMenuAt((int) (x / UIBase.getUIScale()), (int) (y / UIBase.getUIScale()), (int) (s.width / getUIScale()), (int) (s.height / getUIScale()));
		}
	}
	
	public static void openScaledContextMenuAtMouse(FMContextMenu menu) {
		openScaledContextMenuAt(menu, MouseInput.getMouseX(), MouseInput.getMouseY());
	}
	
	public static void renderScaledContextMenu(MatrixStack matrix, FMContextMenu menu) {
		Screen s = MinecraftClient.getInstance().currentScreen;
		if ((s != null) && (menu != null)) {
			
			matrix.push();
			
			matrix.scale(UIBase.getUIScale(), UIBase.getUIScale(), UIBase.getUIScale());
			
			MouseInput.setRenderScale(UIBase.getUIScale());
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			MouseInput.resetRenderScale();
			
			menu.render(matrix, mouseX, mouseY, (int) (s.width / getUIScale()), (int) (s.height / getUIScale()));
			
			matrix.pop();
			
		}
	}

}
