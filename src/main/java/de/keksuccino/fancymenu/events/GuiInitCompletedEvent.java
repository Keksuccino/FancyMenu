package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuiInitCompletedEvent extends Event {
	
	protected GuiScreen screen;
	
	public GuiInitCompletedEvent(GuiScreen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public GuiScreen getGui() {
		return this.screen;
	}
	
}
