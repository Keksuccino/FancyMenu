package de.keksuccino.fancymenu.menu.button;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ButtonData {
	
	private int id;
	private String key;
	private GuiButton button;
	private GuiScreen screen;
	
	public String label;
	public int x;
	public int y;
	public int width;
	public int height;
	
	public ButtonData(GuiButton button, int id, @Nullable String key, GuiScreen fromScreen) {
		this.id = id;
		this.key = key;
		this.button = button;
		this.screen = fromScreen;
		this.label = button.displayString;
		this.x = button.x;
		this.y = button.y;
		this.width = button.width;
		this.height = button.height;
	}
	
	public GuiButton getButton() {
		return button;
	}
	
	public GuiScreen getScreen() {
		return screen;
	}
	
	public String getKey() {
		return key;
	}
	
	public int getId() {
		return id;
	}

}
