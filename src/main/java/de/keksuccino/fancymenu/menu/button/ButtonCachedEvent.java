package de.keksuccino.fancymenu.menu.button;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

public class ButtonCachedEvent extends EventBase {
	
	private Screen screen;
	private List<ButtonData> buttonlist;
	private boolean updated;
	
	public ButtonCachedEvent(Screen screen, List<ButtonData> buttonlist, boolean updated) {
		this.buttonlist = buttonlist;
		this.screen = screen;
		this.updated = updated;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
	public List<ButtonData> getButtonDataList() {
		return this.buttonlist;
	}
	
	public List<AbstractButtonWidget> getWidgetList() {
		List<AbstractButtonWidget> l = new ArrayList<AbstractButtonWidget>();
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
