package de.keksuccino.fancymenu.menu.guiconstruction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
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
		parameters.put(int.class, 0);
		parameters.put(long.class, 0L);
		parameters.put(double.class, 0D);
		parameters.put(float.class, 0F);
		
	}
	
	public static GuiScreen tryToConstruct(String identifier) {
		try {

			if (MenuCustomization.isBlacklistedMenu(identifier)) {
				return null;
			}

			//Update last screen
			parameters.put(GuiScreen.class, Minecraft.getMinecraft().currentScreen);
			//Update player
			parameters.put(EntityPlayer.class, Minecraft.getMinecraft().player);
			if ((Minecraft.getMinecraft().player != null) && (Minecraft.getMinecraft().player.connection != null)) {
				parameters.put(ClientAdvancementManager.class, Minecraft.getMinecraft().player.connection.getAdvancementManager());
			}

			Class<?> gui = Class.forName(identifier, false, GuiConstructor.class.getClassLoader());
			if ((gui != null) && GuiScreen.class.isAssignableFrom(gui)) {
				Constructor<?>[] constructors = gui.getConstructors();
				if ((constructors != null) && (constructors.length > 0)) {
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
							paramInstances.add(parameters.get(p));
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
			if (!parameters.containsKey(par)) {
				return false;
			}
		}
		return true;
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
