package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.MinecraftForge;

public class MenuHandlerRegistry {
	
	private static Map<String, MenuHandlerBase> handlers = new HashMap<String, MenuHandlerBase>();
	private static MenuHandlerBase lastActiveHandler;
	
	public static void registerHandler(MenuHandlerBase handler) {
		if (!handlers.containsKey(handler.getMenuIdentifier())) {
			handlers.put(handler.getMenuIdentifier(), handler);
			MinecraftForge.EVENT_BUS.register(handler);
		}
	}
	
	public static boolean isHandlerRegistered(String menuIdentifier) {
		return handlers.containsKey(menuIdentifier);
	}
	
	public static MenuHandlerBase getLastActiveHandler() {
		return lastActiveHandler;
	}
	
	public static void setActiveHandler(String menuIdentifier) {
		if (menuIdentifier == null) {
			lastActiveHandler = null;
		} else if (isHandlerRegistered(menuIdentifier)) {
			lastActiveHandler = handlers.get(menuIdentifier);
		}
	}

	public static MenuHandlerBase getHandlerFor(Screen screen) {
		return handlers.get(screen.getClass().getName());
	}

}
