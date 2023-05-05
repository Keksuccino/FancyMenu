package de.keksuccino.fancymenu.customization.button;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

public class ButtonData {

	private final long id;
	protected String compatibilityId;
	private final String key;
	private final AbstractWidget button;
	private final Screen screen;
	
	public String label;
	public int x;
	public int y;
	public int width;
	public int height;
	public boolean hasHoverLabel = false;

	public ButtonData(AbstractWidget button, long id, @Nullable String key, Screen fromScreen) {
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
	
	public AbstractWidget getButton() {
		return button;
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

	public String getCompatibilityId() {
		return this.compatibilityId;
	}

	public void setCompatibilityId(String id) {
		this.compatibilityId = id;
	}

}
