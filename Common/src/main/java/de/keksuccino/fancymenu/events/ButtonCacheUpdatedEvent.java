package de.keksuccino.fancymenu.events;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.events.acara.EventBase;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public class ButtonCacheUpdatedEvent extends EventBase {
	
	private final Screen screen;
	private final List<ButtonData> buttonlist;
	private final boolean updated;
	
	public ButtonCacheUpdatedEvent(Screen screen, List<ButtonData> buttonlist, boolean updated) {
		this.buttonlist = buttonlist;
		this.screen = screen;
		this.updated = updated;
	}
	
	public Screen getScreen() {
		return this.screen;
	}
	
	public List<ButtonData> getButtonDataList() {
		return this.buttonlist;
	}
	
	public List<AbstractWidget> getWidgetList() {
		List<AbstractWidget> l = new ArrayList<AbstractWidget>();
		for (ButtonData d : this.buttonlist) {
			l.add(d.getButton());
		}
		return l;
	}
	
	public boolean cacheUpdated() {
		return this.updated;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
