package de.keksuccino.fancymenu.customization.guiconstruction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.guiconstruction.instance.GuiInstance;
import de.keksuccino.fancymenu.customization.guiconstruction.instance.ResourcePacksScreenInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class GuiConstructor {
	
	private static final Map<Class<?>, Object> PARAMETERS = new HashMap<>();
	
	public static void init() {
		
		PARAMETERS.put(Minecraft.class, Minecraft.getInstance());
		PARAMETERS.put(Screen.class, null);
		PARAMETERS.put(Options.class, Minecraft.getInstance().options);
		PARAMETERS.put(LanguageManager.class, Minecraft.getInstance().getLanguageManager());
		PARAMETERS.put(Boolean.class, true);
		PARAMETERS.put(Player.class, null);
		PARAMETERS.put(String.class, "");
		PARAMETERS.put(ClientAdvancements.class, null);
		PARAMETERS.put(Component.class, Component.literal(""));
		PARAMETERS.put(boolean.class, true);
		PARAMETERS.put(int.class, 0);
		PARAMETERS.put(long.class, 0L);
		PARAMETERS.put(double.class, 0D);
		PARAMETERS.put(float.class, 0F);
		
	}
	
	public static Screen tryToConstruct(String identifier) {
		try {

			if (ScreenCustomization.isScreenBlacklisted(identifier)) {
				return null;
			}

			//Update last screen
			PARAMETERS.put(Screen.class, Minecraft.getInstance().screen);
			//Update player
			PARAMETERS.put(Player.class, Minecraft.getInstance().player);
			if ((Minecraft.getInstance().player != null) && (Minecraft.getInstance().player.connection != null)) {
				PARAMETERS.put(ClientAdvancements.class, Minecraft.getInstance().player.connection.getAdvancements());
			}

			Class<?> gui = Class.forName(identifier, false, GuiConstructor.class.getClassLoader());
			if (Screen.class.isAssignableFrom(gui)) {
				Constructor<?>[] constructors = gui.getConstructors();
				if (constructors.length > 0) {
					Constructor<?> con = null;
					//Try to find constructor without parameters
					for (Constructor<?> constructor : constructors) {
						if (constructor.getParameterTypes().length == 0) {
							con = constructor;
							break;
						}
					}
					if (con == null) {
						//Try to find constructor with supported parameters
						for (Constructor<?> constructor : constructors) {
							if (supportsAllParameters(constructor.getParameterTypes())) {
								con = constructor;
								break;
							}
						}
					}
					if (con != null) {
						Class<?>[] params = con.getParameterTypes();
						List<Object> paramInstances = new ArrayList<>();
						for (Class<?> p : params) {
							paramInstances.add(PARAMETERS.get(p));
						}
						return createNewInstance(con, paramInstances, gui);
					}
					return null;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}

	private static boolean supportsAllParameters(Class<?>[] params) {
		for (Class<?> par : params) {
			if (!PARAMETERS.containsKey(par)) {
				return false;
			}
		}
		return true;
	}
	
	private static Screen createNewInstance(Constructor<?> con, List<Object> paras, Class<?> gui) {
		try {

			//Custom loader: ResourcePacksScreen
			if (PackSelectionScreen.class.isAssignableFrom(gui)) {
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
		if (PARAMETERS.containsKey(type)) {
			return PARAMETERS.get(type);
		}
		return null;
	}

}
