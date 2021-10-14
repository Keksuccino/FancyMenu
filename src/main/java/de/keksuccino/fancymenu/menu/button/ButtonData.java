package de.keksuccino.fancymenu.menu.button;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;

public class ButtonData {

	private long id;
	private String key;
	private PressableWidget button;
	private Screen screen;
	private boolean replaced = false;
	
	public String label;
	public int x;
	public int y;
	public int width;
	public int height;

	public ButtonData(PressableWidget button, long id, @Nullable String key, Screen fromScreen) {
		this.id = id;
		this.key = key;
		this.button = button;
		this.screen = fromScreen;
		this.label = button.getMessage().getString();
		this.x = button.x;
		this.y = button.y;
		this.width = button.getWidth();
		this.height = button.getHeight();
	}
	
	public PressableWidget getButton() {
		return button;
	}

	public void replaceButton(PressableWidget w) {
		this.button = w;
		this.replaced = true;
	}

	public boolean isReplaced() {
		return this.replaced;
	}
	
	public Screen getScreen() {
		return screen;
	}
	
	public String getKey() {
		return key;
	}

	public long getId() {
		return id;
	}

}
