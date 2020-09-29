package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ButtonCachedEvent extends Event {
	
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
	
	public List<Widget> getWidgetList() {
		List<Widget> l = new ArrayList<Widget>();
		for (ButtonData d : this.buttonlist) {
			l.add(d.getButton());
		}
		return l;
	}

	/**
	 * Custom childs needs to be added <b>AFTER</b> calling this method!<br>
	 * (Removes custom childs added before calling this method)
	 */
	public void addWidget(Widget w) {
		try {
			Method m = ObfuscationReflectionHelper.findMethod(Screen.class, "addButton", Widget.class);
			m.invoke(this.screen, w);
			
			ButtonCache.addButton(w);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean cacheUpdated() {
		return this.updated;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}

}
