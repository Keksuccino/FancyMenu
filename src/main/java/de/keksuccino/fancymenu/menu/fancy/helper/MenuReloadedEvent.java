package de.keksuccino.fancymenu.menu.fancy.helper;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

public class MenuReloadedEvent extends Event {
	
	private Screen screen;
	
	public MenuReloadedEvent(Screen screen) {
		this.screen = screen;
	}
	
	public Screen getScreen() {
		return this.screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
