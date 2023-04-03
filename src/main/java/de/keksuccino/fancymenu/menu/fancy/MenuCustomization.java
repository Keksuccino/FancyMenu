package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.PlayerEntityRotationScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.*;
import de.keksuccino.konkrete.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.SetupSharingEngine;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerEvents;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.sound.SoundHandler;

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
			MenuHandlerRegistry.registerHandler(new PauseScreenHandler());
			
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

	public static void updateCustomizeableMenuCache() {
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
			PropertiesSet s2 = new PropertiesSet("customizablemenus");
			for (PropertiesSection sec : s.getProperties()) {
				String identifier = null;
				try {
					if (isBlacklistedMenu(sec.getSectionType())) {
						continue;
					}
					if (sec.getSectionType().equals("net.mehvahdjukaar.supplementaries.compat.configured.CustomConfigScreen")) {
						identifier = sec.getSectionType();
					} else if ((sec.getSectionType() != null) && (sec.getSectionType().length() > 5)) {
						Class.forName(sec.getSectionType());
						identifier = sec.getSectionType();
					}
				} catch (Exception e) {}
				if (identifier == null) {
					identifier = getValidMenuIdentifierFor(sec.getSectionType());
				}
				s2.addProperties(new PropertiesSection(identifier));
			}
			customizableMenus = s2;
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

	public static String getValidMenuIdentifierFor(String identifier) {
		if (isBlacklistedMenu(identifier)) {
			return identifier;
		}
		if (CustomGuiLoader.guiExists(identifier)) {
			return identifier;
		}
		SetupSharingEngine.MenuIdentifierDatabase db = SetupSharingEngine.getIdentifierDatabase();
		try {
			Class.forName(identifier);
			return identifier;
		} catch (Exception e) {}
		if (db != null) {
			String s = db.findValidIdentifierFor(identifier);
			if (s != null) {
				return s;
			}
		}
		return identifier;
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
		if (Minecraft.getInstance().screen != screen) {
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
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			if (isMenuCustomizable(s)) {
				setIsNewMenu(true);
				SoftMenuReloadEvent e = new SoftMenuReloadEvent(s);
				Konkrete.getEventHandler().callEventsFor(e);
				Minecraft.getInstance().setScreen(s);
			}
		}
	}

	public static void enableLayout(String path) {
		try {
			File f = new File(path);
			String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationPath().getPath(), Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(FancyMenu.getCustomizationPath().getPath() + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CustomizationHelper.reloadSystemAndMenu();
	}

	public static void enableLayout(MenuCustomizationProperties.LayoutProperties layout) {
		if (layout.path != null) {
			enableLayout(layout.path);
		}
	}

	public static void disableLayout(String path) {
		try {
			File f = new File(path);
			String disPath = FancyMenu.getCustomizationPath().getPath() + "/.disabled";
			String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(disPath + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CustomizationHelper.reloadSystemAndMenu();
	}

	public static void disableLayout(MenuCustomizationProperties.LayoutProperties layout) {
		if (layout.path != null) {
			disableLayout(layout.path);
		}
	}

	public static boolean isBlacklistedMenu(String menuIdentifierOrPartOfIdentifier) {
		if (menuIdentifierOrPartOfIdentifier.startsWith(PlayerEntityRotationScreen.class.getName())) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.startsWith("com.simibubi.create.")) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.startsWith("de.keksuccino.panoramica.")) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.startsWith("com.github.alexthe666.alexsmobs.")) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.equals(TextEditorScreen.class.getName())) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.startsWith("de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement.")) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.equals(ConfirmationScreen.class.getName())) {
			return true;
		}
		if (menuIdentifierOrPartOfIdentifier.startsWith("de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions.")) {
			return true;
		}
		return false;
	}

	public static String getAbsoluteGameDirectoryPath(String path) {
		try {
			path = path.replace("\\", "/");
			String gameDir = FancyMenu.getGameDirectory().getAbsolutePath().replace("\\", "/");
			if (!path.startsWith(gameDir)) {
				String fixed = gameDir + "/" + path;
				return fixed;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return path;
	}
	
}
