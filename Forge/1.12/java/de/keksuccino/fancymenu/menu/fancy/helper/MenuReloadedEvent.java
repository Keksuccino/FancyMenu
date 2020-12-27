package de.keksuccino.fancymenu.menu.fancy.helper;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;

public class MenuReloadedEvent extends Event {
	
	private GuiScreen screen;
	
	public MenuReloadedEvent(GuiScreen screen) {
		this.screen = screen;
	}
	
	public GuiScreen getGui() {
		return this.screen;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
