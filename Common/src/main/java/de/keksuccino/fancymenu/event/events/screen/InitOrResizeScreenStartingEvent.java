package de.keksuccino.fancymenu.event.events.screen;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class InitOrResizeScreenStartingEvent extends EventBase {

	protected Screen screen;

	public InitOrResizeScreenStartingEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public Screen getScreen() {
		return this.screen;
	}
	
}
