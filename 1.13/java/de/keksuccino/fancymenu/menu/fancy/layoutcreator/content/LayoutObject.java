package de.keksuccino.fancymenu.menu.fancy.layoutcreator.content;

import java.awt.Color;

import org.lwjgl.glfw.GLFW;

import de.keksuccino.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

public class LayoutObject extends Gui {
	
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean hovered = false;
	private boolean focused = false;
	
	private static long hResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	private static long vResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	private static long normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CURSOR_NORMAL);
	
	public LayoutObject(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public void render(int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);
		this.updateFocused();
		
		if (this.focused) {
			this.renderBorder(mouseX, mouseY);
		}
		
		//TODO REMOVE DEBUG
		if (this.isHovered()) {
			System.out.println("is hovered");
		}
		if (this.isLeftClicked()) {
			System.out.println("is left-clicked");
		}
	}
	
	private void renderBorder(int mouseX, int mouseY) {
		//horizontal line top
		GuiScreen.drawRect(this.x, this.y, this.x + this.width, this.y + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		GuiScreen.drawRect(this.x, this.y + this.height, this.x + this.width + 1, this.y + this.height + 1, Color.BLUE.getRGB());
		//vertical line left
		GuiScreen.drawRect(this.x, this.y, this.x + 1, this.y + this.height, Color.BLUE.getRGB());
		//vertical line right
		GuiScreen.drawRect(this.x + this.width, this.y, this.x + this.width + 1, this.y + this.height, Color.BLUE.getRGB());
		
		int w = 4;
		int h = 4;
		
		int yHorizontal = this.y + (this.height / 2) - (h / 2);
		int xHorizontalLeft = this.x - (w / 2);
		int xHorizontalRight = this.x + this.width - (w / 2) + 1;
		
		int xVertical = this.x + (this.width / 2) - (w / 2);
		int yVerticalTop = this.y - (h / 2);
		int yVerticalBottom = this.y + this.height - (h / 2) + 1;
		
		//cube left
		GuiScreen.drawRect(xHorizontalLeft, yHorizontal, xHorizontalLeft + w, yHorizontal + h, Color.BLUE.getRGB());
		//cube right
		GuiScreen.drawRect(xHorizontalRight, yHorizontal, xHorizontalRight + w, yHorizontal + h, Color.BLUE.getRGB());
		//cube top
		GuiScreen.drawRect(xVertical, yVerticalTop, xVertical + w, yVerticalTop + h, Color.BLUE.getRGB());
		//cube bottom
		GuiScreen.drawRect(xVertical, yVerticalBottom, xVertical + w, yVerticalBottom + h, Color.BLUE.getRGB());
		
		if ((mouseX >= xHorizontalLeft) && (mouseX <= xHorizontalLeft + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
			this.resizeHorizontal(mouseX, mouseY);
		} else if ((mouseX >= xHorizontalRight) && (mouseX <= xHorizontalRight + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
			this.resizeHorizontal(mouseX, mouseY);
		} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalTop) && (mouseY <= yVerticalTop + h)) {
			this.resizeVertical(mouseX, mouseY);
		} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalBottom) && (mouseY <= yVerticalBottom + h)) {
			this.resizeVertical(mouseX, mouseY);
		} else {
			this.resetResizeCursor();
		}
	}
	
	private void resizeVertical(int mouseX, int mouseY) {
		GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), vResizeCursor);
	}
	
	private void resizeHorizontal(int mouseX, int mouseY) {
		GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), hResizeCursor);
	}
	
	private void resetResizeCursor() {
		GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), normalCursor);
	}
	
	private void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.x) && (mouseX <= this.x + this.width) && (mouseY >= this.y) && mouseY <= this.y + this.height) {
			this.hovered = true;
		} else {
			this.hovered = false;
		}
	}
	
	private void updateFocused() {
		if (this.isLeftClicked()) {
			this.focused = true;
		}
		if (!this.isHovered() && MouseInput.isLeftMouseDown()) {
			this.focused = false;
		}
	}
	
	public boolean isLeftClicked() {
		return (this.isHovered() && MouseInput.isLeftMouseDown());
	}
	
	public boolean isRightClicked() {
		return (this.isHovered() && MouseInput.isRightMouseDown());
	}
	
	public boolean isHovered() {
		return this.hovered;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

}
