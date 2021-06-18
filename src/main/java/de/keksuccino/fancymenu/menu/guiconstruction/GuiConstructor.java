package de.keksuccino.fancymenu.menu.guiconstruction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.guiconstruction.instance.GuiInstance;
import de.keksuccino.fancymenu.menu.guiconstruction.instance.ResourcePacksScreenInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("resource")
public class GuiConstructor {
	
	private static Map<Class<?>, Object> parameters = new HashMap<Class<?>, Object>();
	
	public static void init() {
		
		parameters.put(MinecraftClient.class, MinecraftClient.getInstance());
		parameters.put(Screen.class, null);
		parameters.put(GameOptions.class, MinecraftClient.getInstance().options);
		parameters.put(LanguageManager.class, MinecraftClient.getInstance().getLanguageManager());
		parameters.put(boolean.class, true);
		parameters.put(PlayerEntity.class, null);
		parameters.put(String.class, "");
		parameters.put(ClientAdvancementManager.class, null);
		
	}
	
	public static Screen tryToConstruct(String identifier) {
		try {
			//Update last screen
			parameters.put(Screen.class, MinecraftClient.getInstance().currentScreen);
			//Update player
			parameters.put(PlayerEntity.class, MinecraftClient.getInstance().player);
			if ((MinecraftClient.getInstance().player != null) && (MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler() != null)) {
				parameters.put(ClientAdvancementManager.class, MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler());
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
