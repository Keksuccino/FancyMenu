package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Event;

public class GuiInitCompletedEvent extends Event {
	
	protected Screen screen;
	
	public GuiInitCompletedEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
}
