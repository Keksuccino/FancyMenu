package de.keksuccino.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;

public class MainMenu {

	public static boolean isCurrentScreen() {
		return (Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu);
	}

}
