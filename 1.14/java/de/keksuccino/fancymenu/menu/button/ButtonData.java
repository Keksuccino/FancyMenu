package de.keksuccino.fancymenu.menu.button;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class ButtonData {
	
	private int id;
	private String key;
	private Widget button;
	private Screen screen;
	
	public String label;
	public int x;
	public int y;
	public int width;
	public int height;
	
	public ButtonData(Widget button, int id, @Nullable String key, Screen fromScreen) {
		this.id = id;
		this.key = key;
		this.button = button;
		this.screen = fromScreen;
		this.label = button.getMessage();
		this.x = button.x;
		this.y = button.y;
		this.width = button.getWidth();
		this.height = button.getHeight();
	}
	
	public Widget getButton() {
		return button;
	}
	
	public Screen getScreen() {
		return screen;
	}
	
	public String getKey() {
		return key;
	}
	
	public int getId() {
		return id;
	}

}
