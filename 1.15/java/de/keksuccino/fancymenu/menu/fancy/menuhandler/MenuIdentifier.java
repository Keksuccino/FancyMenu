package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import net.minecraft.client.gui.screen.MainMenuScreen;

public class MenuIdentifier {
	
	public static String getIdentifier(String classPath) {
		if (MainMenuScreen.class.getName().equalsIgnoreCase(classPath)) {
			return "%mainscreen%";
		}
		if (MainMenuScreen.class.getName().equalsIgnoreCase(classPath)) {
			return "%mainscreen%";
		}
		
		return classPath;
	}

}
