package de.keksuccino.fancymenu.menu.fancy.helper;

import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.eventbus.api.Event;

public class MenuReloadedEvent extends Event {
	
	private Screen screen;
	
	public MenuReloadedEvent(Screen screen) {
		this.screen = screen;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
