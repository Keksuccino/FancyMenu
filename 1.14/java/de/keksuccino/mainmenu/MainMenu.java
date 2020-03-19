package de.keksuccino.mainmenu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.fml.BrandingControl;

public class MainMenu {
	
	public static void clearBranding() {
		try {
			Field f = BrandingControl.class.getDeclaredField("brandings");
			f.setAccessible(true);
			List<String> l = new ArrayList<String>();
			f.set(BrandingControl.class, l);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static boolean isCurrentScreen() {
		return (Minecraft.getInstance().currentScreen instanceof MainMenuScreen);
	}

}
