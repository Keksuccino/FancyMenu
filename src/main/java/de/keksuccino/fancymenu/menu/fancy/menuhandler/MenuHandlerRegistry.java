package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.util.HashMap;
import java.util.Map;

import de.keksuccino.konkrete.Konkrete;

public class MenuHandlerRegistry {
	
	private static Map<String, MenuHandlerBase> handlers = new HashMap<String, MenuHandlerBase>();
	private static MenuHandlerBase lastActiveHandler;
	
	public static void registerHandler(MenuHandlerBase handler) {
		if (!handlers.containsKey(handler.getMenuIdentifier())) {
			handlers.put(handler.getMenuIdentifier(), handler);
			Konkrete.getEventHandler().registerEventsFrom(handler);
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
}
