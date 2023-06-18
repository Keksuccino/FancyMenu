package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
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
