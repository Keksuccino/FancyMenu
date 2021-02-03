package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ButtonCachedEvent extends Event {
	
	private GuiScreen screen;
	private List<ButtonData> buttonlist;
	private boolean updated;
	
	public ButtonCachedEvent(GuiScreen screen, List<ButtonData> buttonlist, boolean updated) {
		this.buttonlist = buttonlist;
		this.screen = screen;
		this.updated = updated;
	}
	
	public GuiScreen getGui() {
		return this.screen;
	}
	
	public List<ButtonData> getButtonDataList() {
		return this.buttonlist;
	}
	
	public List<GuiButton> getButtonList() {
		List<GuiButton> l = new ArrayList<GuiButton>();
		for (ButtonData d : this.buttonlist) {
			l.add(d.getButton());
		}
		return l;
	}
	
	/**
	 * Custom childs needs to be added <b>AFTER</b> calling this method!<br>
	 * (Removes custom childs added before calling this method)
	 */
	public void addButton(GuiButton w) {
		try {
			Field f = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			if (f != null) {
				List<GuiButton> l = (List<GuiButton>) f.get(this.screen);
				l.add(w);
				
				ButtonCache.addButton(w);
			}
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
