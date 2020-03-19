package de.keksuccino.fancymenu.menu.button;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ButtonData {
	
	private int id;
	private String key;
	private String name;
	private GuiButton button;
	private GuiScreen screen;
	
	public ButtonData(GuiButton button, int id, String name, @Nullable String key, GuiScreen fromScreen) {
		this.id = id;
		this.key = key;
		this.name = name;
		this.button = button;
		this.screen = fromScreen;
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
	
	public String getName() {
		return name;
	}

}
