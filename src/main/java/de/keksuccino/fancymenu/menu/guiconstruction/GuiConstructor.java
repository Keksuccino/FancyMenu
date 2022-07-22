package de.keksuccino.fancymenu.menu.guiconstruction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.guiconstruction.instance.GuiInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class GuiConstructor {
	
	private static Map<Class<?>, Object> parameters = new HashMap<Class<?>, Object>();
	
	public static void init() {
		
		parameters.put(Minecraft.class, Minecraft.getMinecraft());
		parameters.put(GuiScreen.class, null);
		parameters.put(GameSettings.class, Minecraft.getMinecraft().gameSettings);
		parameters.put(LanguageManager.class, Minecraft.getMinecraft().getLanguageManager());
		parameters.put(Boolean.class, true);
		parameters.put(EntityPlayer.class, null);
		parameters.put(String.class, "");
		parameters.put(ClientAdvancementManager.class, null);
		parameters.put(ITextComponent.class, new TextComponentString(""));
		parameters.put(boolean.class, true);
		parameters.put(int.class, 1);
		parameters.put(long.class, 1L);
		parameters.put(double.class, 1D);
		parameters.put(float.class, 1F);
		
	}
	
	public static GuiScreen tryToConstruct(String identifier) {
		try {
			//Update last screen
			parameters.put(GuiScreen.class, Minecraft.getMinecraft().currentScreen);
			//Update player
			parameters.put(EntityPlayer.class, Minecraft.getMinecraft().player);
			if ((Minecraft.getMinecraft().player != null) && (Minecraft.getMinecraft().player.connection != null)) {
				parameters.put(ClientAdvancementManager.class, Minecraft.getMinecraft().player.connection.getAdvancementManager());
			}
			
			Class<?> gui = Class.forName(identifier);
			if ((gui != null) && GuiScreen.class.isAssignableFrom(gui)) {
				Constructor<?>[] c = gui.getConstructors();
				
				if ((c != null) && (c.length > 0)) {
					Constructor<?> con = c[0];
					Class<?>[] pars = con.getParameterTypes();
					List<Object> pars2 = new ArrayList<Object>();
					
					for (Class<?> par : pars) {
						if (parameters.containsKey(par)) {
							pars2.add(parameters.get(par));
						}
					}
					
					return createNewInstance(con, pars2, gui);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	private static GuiScreen createNewInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		try {
			
			//Default loader
			return new GuiInstance(con, paras, gui).getInstance();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Object findParameterOfType(Class<?> type) {
		if (parameters.containsKey(type)) {
			return parameters.get(type);
		}
		return null;
	}

}
