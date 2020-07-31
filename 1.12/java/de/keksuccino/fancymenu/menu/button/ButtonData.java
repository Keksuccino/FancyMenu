package de.keksuccino.fancymenu.menu.button;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ButtonData {
	
	private long id;
	private String key;
	private GuiButton button;
	private GuiScreen screen;
	private boolean replaced = false;
	
	public String label;
	public int x;
	public int y;
	public int width;
	public int height;
	
	public ButtonData(GuiButton button, long id, @Nullable String key, GuiScreen fromScreen) {
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
	
	public void replaceButton(GuiButton w) {
		this.button = w;
		this.replaced = true;
	}

	public boolean isReplaced() {
		return this.replaced;
	}
	
	public GuiScreen getScreen() {
		return screen;
	}
	
	public String getKey() {
		return key;
	}
	
	public long getId() {
		return id;
	}

}
