package de.keksuccino.fancymenu.menu.button;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class ButtonData {
	
	private int id;
	private String key;
	private String name;
	private Widget button;
	private Screen screen;
	
	public ButtonData(Widget button, int id, String name, @Nullable String key, Screen fromScreen) {
		this.id = id;
		this.key = key;
		this.name = name;
		this.button = button;
		this.screen = fromScreen;
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
	
	public String getName() {
		return name;
	}

}
