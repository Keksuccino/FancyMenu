package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;

public class MenuReloadedEvent extends EventBase {
	
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
