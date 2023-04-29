package de.keksuccino.fancymenu.events;

import de.keksuccino.fancymenu.events.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class MenuReloadEvent extends EventBase {
	
	private final Screen screen;
	
	public MenuReloadEvent(Screen screen) {
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
