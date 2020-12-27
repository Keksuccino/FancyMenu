package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.konkrete.events.EventBase;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
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

	/**
	 * Custom childs needs to be added <b>AFTER</b> calling this method!<br>
	 * (Removes custom childs added before calling this method)
	 */
	public void addWidget(AbstractButtonWidget w) {
		try {
			Method m = ReflectionHelper.findMethod(Screen.class, "addButton", "method_25411", AbstractButtonWidget.class);
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
