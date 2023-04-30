package de.keksuccino.fancymenu.customization.menuhandler;

import java.util.HashMap;
import java.util.Map;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import net.minecraft.client.gui.screens.Screen;

public class MenuHandlerRegistry {
	
	private static Map<String, MenuHandlerBase> handlers = new HashMap<String, MenuHandlerBase>();
	private static MenuHandlerBase lastActiveHandler;
	
	public static void registerHandler(MenuHandlerBase handler) {
		if (!handlers.containsKey(handler.getMenuIdentifier())) {
			handlers.put(handler.getMenuIdentifier(), handler);
			//TODO stop registering every single handler and instead handle events of active handler in separate class
			EventHandler.INSTANCE.registerListenersOf(handler);
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
