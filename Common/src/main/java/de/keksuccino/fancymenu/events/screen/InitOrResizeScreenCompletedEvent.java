package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.events.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class InitOrResizeScreenCompletedEvent extends EventBase {
	
	protected Screen screen;
	
	public InitOrResizeScreenCompletedEvent(Screen screen) {
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
