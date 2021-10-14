package de.keksuccino.fancymenu.menu.guiconstruction.instance;

import java.lang.reflect.Constructor;
import java.util.List;

import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import net.minecraft.client.gui.GuiScreen;

public class GuiInstance {

	protected GuiScreen instance;
	
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
				
				this.instance = (GuiScreen) this.con.newInstance();
				
			} else {
				
				this.instance = (GuiScreen) this.con.newInstance(paras.toArray(new Object[0]));
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public GuiScreen getInstance() {
		return this.instance;
	}
	
	protected Object findParameter(Class<?> type) {
		return GuiConstructor.findParameterOfType(type);
	}
	
}
