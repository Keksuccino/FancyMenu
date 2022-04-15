package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;

public class GuiInstance {

	protected Screen instance;
	
	protected Constructor<?> con;
	protected List<Object> paras;
	protected Class<?> gui;
	
	public GuiInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		this.con = con;
		this.paras = paras;
		this.gui = gui;
		
		this.createInstance();
	}
	
	protected void createInstance() {
		try {
			if (paras.isEmpty()) {
				
				this.instance = (Screen) this.con.newInstance();
				
			} else {
				
				this.instance = (Screen) this.con.newInstance(paras.toArray(new Object[0]));
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Screen getInstance() {
		return this.instance;
	}
	
	protected Object findParameter(Class<?> type) {
		return GuiConstructor.findParameterOfType(type);
	}
	
}
