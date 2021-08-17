package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerEvents;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.DummyCoreMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MoreRefinedStorageMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.WorldLoadingScreenHandler;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class MenuCustomization {
	
	private static PropertiesSet customizableMenus;
	
	private static boolean initDone = false;
	private static List<String> sounds = new ArrayList<String>();
	
	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");
	
	protected static boolean isCurrentScrollable = false;
	protected static boolean isNewMenu = true;
	protected static MenuCustomizationEvents eventsInstance = new MenuCustomizationEvents();

	public static boolean isLoadingScreen = true;
	
	public static void init() {
		if (!initDone) {
			//Registering (base) events for the MenuCustomization system
			Konkrete.getEventHandler().registerEventsFrom(eventsInstance);
			
			//Registering all custom menu handlers
			MenuHandlerRegistry.registerHandler(new MainMenuHandler());
			MenuHandlerRegistry.registerHandler(new MoreRefinedStorageMainHandler());
			MenuHandlerRegistry.registerHandler(new DummyCoreMainHandler());
			MenuHandlerRegistry.registerHandler(new WorldLoadingScreenHandler());
			
			//Registering event to automatically register handlers for all menus (its necessary to do this AFTER registering custom handlers!)
			Konkrete.getEventHandler().registerEventsFrom(new MenuHandlerEvents());
			
			CustomizationHelper.init();
			
			//Registering the update event for the button cache
			Konkrete.getEventHandler().registerEventsFrom(new ButtonCache());
			
			//Caching menu customization properties from config/fancymain/customization
			MenuCustomizationProperties.loadProperties();

			updateCustomizeableMenuCache();
			
			initDone = true;
		}
	}

	private static void updateCustomizeableMenuCache() {
		try {
			if (!CUSTOMIZABLE_MENUS_FILE.exists()) {
				CUSTOMIZABLE_MENUS_FILE.createNewFile();
				PropertiesSerializer.writeProperties(new PropertiesSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			PropertiesSet s = PropertiesSerializer.getProperties(CUSTOMIZABLE_MENUS_FILE.getPath());
			if (s == null) {
				PropertiesSerializer.writeProperties(new PropertiesSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
				s = PropertiesSerializer.getProperties(CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			customizableMenus = s;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void syncCustomizeableMenusToFile() {
		PropertiesSerializer.writeProperties(customizableMenus, CUSTOMIZABLE_MENUS_FILE.getPath());
	}

	public static void enableCustomizationForMenu(Screen menu) {
		if (menu != null) {
			if (!(menu instanceof CustomGuiBase)) {
				String identifier = menu.getClass().getName();
				if ((identifier != null) && (customizableMenus != null)) {
					PropertiesSection sec = new PropertiesSection(identifier);
					customizableMenus.addProperties(sec);
					syncCustomizeableMenusToFile();
				}
			}
		}
	}

	public static void disableCustomizationForMenu(Screen menu) {
		if (menu != null) {
			if (!(menu instanceof CustomGuiBase)) {
				String identifier = menu.getClass().getName();
				if ((identifier != null) && (customizableMenus != null)) {
					List<PropertiesSection> l = new ArrayList<PropertiesSection>();
					for (PropertiesSection sec : customizableMenus.getProperties()) {
						if (!sec.getSectionType().equals(identifier)) {
							l.add(sec);
						}
					}
					customizableMenus = new PropertiesSet("customizablemenus");
					for (PropertiesSection sec : l) {
						customizableMenus.addProperties(sec);
					}
					syncCustomizeableMenusToFile();
				}
			}
		}
	}

	public static boolean isMenuCustomizable(Screen menu) {
		if (menu != null) {
			if (menu instanceof CustomGuiBase) {
				return true;
			}
			String identifier = menu.getClass().getName();
			if ((identifier != null) && (customizableMenus != null)) {
				List<PropertiesSection> s = customizableMenus.getPropertiesOfType(identifier);
				if ((s != null) && !s.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static void reload() {
		if (initDone) {
			updateCustomizeableMenuCache();
			//Resets itself automatically and can be used for both loading and reloading
			MenuCustomizationProperties.loadProperties();
		}
	}
	
	public static boolean isValidScreen(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (MinecraftClient.getInstance().currentScreen != screen) {
			return false;
		}
		return true;
	}
	
	public static void registerSound(String key, String path) {
		if (!sounds.contains(key)) {
			sounds.add(key);
		}
		SoundHandler.registerSound(key, path);
	}
	
	public static void unregisterSound(String key) {
		if (sounds.contains(key)) {
			sounds.remove(key);
		}
		SoundHandler.unregisterSound(key);
	}
	
	public static void stopSounds() {
		for (String s : sounds) {
			SoundHandler.stopSound(s);
		}
	}
	
	public static void resetSounds() {
		for (String s : sounds) {
			SoundHandler.resetSound(s);
		}
	}

	public static boolean isSoundRegistered(String key) {
		return sounds.contains(key);
	}

	public static List<String> getSounds() {
		return sounds;
	}
	
	public static boolean isCurrentMenuScrollable() {
		return isCurrentScrollable;
	}

	public static String generateRandomActionId() {
		long ms = System.currentTimeMillis();
		String s = UUID.randomUUID().toString();
		return s + ms;
	}
	
	public static boolean isNewMenu() {
		return isNewMenu;
	}

	public static void setIsNewMenu(boolean b) {
		isNewMenu = b;
		eventsInstance.lastScreen = null;
	}

	public static void reloadCurrentMenu() {
		Screen s = MinecraftClient.getInstance().currentScreen;
		if (s != null) {
			if (isMenuCustomizable(s)) {
				setIsNewMenu(true);
				SoftMenuReloadEvent e = new SoftMenuReloadEvent(s);
				Konkrete.getEventHandler().callEventsFor(e);
				MinecraftClient.getInstance().setScreen(s);
			}
		}
	}
	
}
