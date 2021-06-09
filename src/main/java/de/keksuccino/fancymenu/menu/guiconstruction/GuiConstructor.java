package de.keksuccino.fancymenu.menu.guiconstruction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.guiconstruction.instance.GuiInstance;
import de.keksuccino.fancymenu.menu.guiconstruction.instance.ResourcePacksScreenInstance;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.PackScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.entity.player.PlayerEntity;

public class GuiConstructor {
	
	private static Map<Class<?>, Object> parameters = new HashMap<Class<?>, Object>();
	
	public static void init() {
		
		parameters.put(Minecraft.class, Minecraft.getInstance());
		parameters.put(Screen.class, null);
		parameters.put(GameSettings.class, Minecraft.getInstance().gameSettings);
		parameters.put(LanguageManager.class, Minecraft.getInstance().getLanguageManager());
		parameters.put(boolean.class, true);
		parameters.put(PlayerEntity.class, null);
		parameters.put(String.class, "");
		//TODO übernehmen
		parameters.put(ClientAdvancementManager.class, null);
		
	}
	
	public static Screen tryToConstruct(String identifier) {
		try {
			//Update last screen
			parameters.put(Screen.class, Minecraft.getInstance().currentScreen);
			//Update player
			parameters.put(PlayerEntity.class, Minecraft.getInstance().player);
			//TODO übernehmen
			if ((Minecraft.getInstance().player != null) && (Minecraft.getInstance().player.connection != null)) {
				parameters.put(ClientAdvancementManager.class, Minecraft.getInstance().player.connection.getAdvancementManager());
			}
			
			Class<?> gui = Class.forName(identifier);
			if ((gui != null) && Screen.class.isAssignableFrom(gui)) {
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
	
	private static Screen createNewInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		try {

			//Custom loader: ResourcePacksScreen
			if (PackScreen.class.isAssignableFrom(gui)) {
				return new ResourcePacksScreenInstance(con, paras, gui).getInstance();
			}
			
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
