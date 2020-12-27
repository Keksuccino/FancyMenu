package de.keksuccino.fancymenu.menu.button;

import java.lang.reflect.Method;
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
			Method m = ReflectionHelper.findMethod(GuiScreen.class, "addButton", "func_189646_b", GuiButton.class);
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
