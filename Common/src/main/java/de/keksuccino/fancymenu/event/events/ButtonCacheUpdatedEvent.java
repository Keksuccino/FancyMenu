package de.keksuccino.fancymenu.event.events;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.event.acara.EventBase;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public class ButtonCacheUpdatedEvent extends EventBase {
	
	private final Screen screen;
	private final List<WidgetMeta> buttonlist;
	private final boolean updated;
	
	public ButtonCacheUpdatedEvent(Screen screen, List<WidgetMeta> buttonlist, boolean updated) {
		this.buttonlist = buttonlist;
		this.screen = screen;
		this.updated = updated;
	}
	
	public Screen getScreen() {
		return this.screen;
	}
	
	public List<WidgetMeta> getButtonDataList() {
		return this.buttonlist;
	}
	
	public List<AbstractWidget> getWidgetList() {
		List<AbstractWidget> l = new ArrayList<AbstractWidget>();
		for (WidgetMeta d : this.buttonlist) {
			l.add(d.getWidget());
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
