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
		this.label = button.func_230458_i_().getString();
		this.x = button.field_230690_l_ ;
		this.y = button.field_230691_m_;
		this.width = button.func_230998_h_();
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
