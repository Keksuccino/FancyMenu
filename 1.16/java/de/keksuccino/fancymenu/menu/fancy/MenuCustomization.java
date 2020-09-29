package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.loadingdarkmode.LoadingDarkmodeEvents;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerEvents;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.DummyCoreMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MoreRefinedStorageMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.controls.ControlsMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.languagesettings.LanguageMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.serverselection.ServerSelectionMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.videosettings.VideoSettingsMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection.WorldSelectionMenuHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.client.gui.NotificationModUpdateScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.versions.mcp.MCPVersion;

public class MenuCustomization {
	
	private static boolean initDone = false;
	private static List<String> sounds = new ArrayList<String>();

	private static Map<String, PropertiesSection> excludedmenus = new HashMap<String, PropertiesSection>();
	public static final File EXCLUDED_MENUS_FILE = new File("config/fancymenu/excludedmenus.txt");
	
	public static void init() {
		if (!initDone) {
			//Registering (base) events for the MenuCustomization system
			MinecraftForge.EVENT_BUS.register(new MenuCustomizationEvents());
			
			//Registering all custom menu handlers
			MenuHandlerRegistry.registerHandler(new MainMenuHandler());
			MenuHandlerRegistry.registerHandler(new MoreRefinedStorageMainHandler());
			MenuHandlerRegistry.registerHandler(new DummyCoreMainHandler());
			
			if (!FancyMenu.config.getOrDefault("softmode", false)) {
				MenuHandlerRegistry.registerHandler(new WorldSelectionMenuHandler());
				MenuHandlerRegistry.registerHandler(new ServerSelectionMenuHandler());
				MenuHandlerRegistry.registerHandler(new ControlsMenuHandler());
				MenuHandlerRegistry.registerHandler(new LanguageMenuHandler());
				MenuHandlerRegistry.registerHandler(new VideoSettingsMenuHandler());
			}
			
			//Registering event to automatically register handlers for all menus (its necessary to do this AFTER registering custom handlers!)
			MinecraftForge.EVENT_BUS.register(new MenuHandlerEvents());
			
			CustomizationHelper.init();
			
			//Registering the update event for the button cache
			MinecraftForge.EVENT_BUS.register(new ButtonCache());
			
			//Caching menu customization properties from config/fancymain/customization
			MenuCustomizationProperties.loadProperties();
			
			LoadingDarkmodeEvents.init();

			reloadExcludedMenus();
			
			initDone = true;
		}
	}
	
	public static void reload() {
		if (initDone) {
			//Resets itself automatically and can be used for both loading and reloading
			MenuCustomizationProperties.loadProperties();
		}
	}
	
	public static boolean isValidScreen(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (screen instanceof NotificationModUpdateScreen) {
			return false;
		}
		if (screen instanceof RealmsScreen) {
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

	public static boolean containsCalculations(PropertiesSet properties) {
		for (PropertiesSection s : properties.getPropertiesOfType("customization")) {
			String width = s.getEntryValue("width");
			String height = s.getEntryValue("height");
			String x = s.getEntryValue("x");
			String y = s.getEntryValue("y");
			String posX = s.getEntryValue("posX");
			String posY = s.getEntryValue("posY");
			if ((width != null) && !MathUtils.isInteger(width)) {
				return true;
			}
			if ((height != null) && !MathUtils.isInteger(height)) {
				return true;
			}
			if ((x != null) && !MathUtils.isInteger(x)) {
				return true;
			}
			if ((y != null) && !MathUtils.isInteger(y)) {
				return true;
			}
			if ((posX != null) && !MathUtils.isInteger(posX)) {
				return true;
			}
			if ((posY != null) && !MathUtils.isInteger(posY)) {
				return true;
			}
		}
		return false;
	}

	public static String convertString(String s) {
		int width = 0;
		int height = 0;
		String playername = "";
		String playeruuid = "";
		String mcversion = MCPVersion.getMCVersion();
		if (Minecraft.getInstance().player != null) {
			playername = Minecraft.getInstance().player.getName().getString();
			playeruuid = Minecraft.getInstance().player.getUniqueID().toString();
		}
		if (Minecraft.getInstance().currentScreen != null) {
			width = Minecraft.getInstance().currentScreen.width;
			height = Minecraft.getInstance().currentScreen.height;
		}
		
		//Convert &-formatcodes to real ones
		s = StringUtils.convertFormatCodes(s, "&", "§");
		
		//Replace height and width placeholders
		s = s.replace("%guiwidth%", "" + width);
		s = s.replace("%guiheight%", "" + height);
		
		//Replace player name and uuid placeholders
		s = s.replace("%playername%", playername);
		s = s.replace("%playeruuid%", playeruuid);
		
		//Replace mc version placeholder
		s = s.replace("%mcversion%", mcversion);
		
		return s;
	}

	public static void reloadExcludedMenus() {
		try {
			excludedmenus.clear();
			
			PropertiesSet s = PropertiesSerializer.getProperties(EXCLUDED_MENUS_FILE.getPath());
			if (s == null) {
				s = new PropertiesSet("excludedmenus");
				PropertiesSection sec = new PropertiesSection("some.example.menu.identifier.to.exclude");
				sec.addEntry("mode", "soft");
				sec.addEntry("startswith", "false");
				s.addProperties(sec);
				PropertiesSerializer.writeProperties(s, EXCLUDED_MENUS_FILE.getPath());
			}
			
			for (PropertiesSection ps : s.getProperties()) {
				String excludefrom = ps.getEntryValue("mode");
				if ((excludefrom != null) && (excludefrom.equalsIgnoreCase("full") || excludefrom.equalsIgnoreCase("soft"))) {
					excludedmenus.put(ps.getSectionType(),  ps);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addExcludedMenu(String menuIdentifier, ExcludeMode mode, boolean startsWith) {
		reloadExcludedMenus();
		
		removeExcludedMenu(menuIdentifier);
		
		PropertiesSet s = new PropertiesSet("excludedmenus");
		
		for (PropertiesSection ps : excludedmenus.values()) {
			s.addProperties(ps);
		}
		
		PropertiesSection sec = new PropertiesSection(menuIdentifier);
		sec.addEntry("mode", mode.getMode());
		sec.addEntry("startswith", "" + startsWith);
		s.addProperties(sec);
		
		excludedmenus.put(menuIdentifier, sec);
		
		PropertiesSerializer.writeProperties(s, EXCLUDED_MENUS_FILE.getPath());
	}

	public static void removeExcludedMenu(String menuIdentifier) {
		reloadExcludedMenus();
		
		if (excludedmenus.containsKey(menuIdentifier)) {
			excludedmenus.remove(menuIdentifier);
			
			PropertiesSet s = new PropertiesSet("excludedmenus");
			
			for (PropertiesSection ps : excludedmenus.values()) {
				s.addProperties(ps);
			}
			
			PropertiesSerializer.writeProperties(s, EXCLUDED_MENUS_FILE.getPath());
		}
	}

	/**
	 * Checks if the given menu identifier is in the list of excluded menus and returns the {@link PropertiesSection}
	 * entry if it is excluded or NULL if no entry was found for this identifier.
	 */
	public static PropertiesSection getExcludedMenu(String menuIdentifier) {
		for (PropertiesSection sec : excludedmenus.values()) {
			String startswith = sec.getEntryValue("startswith");
			if (startswith == null) {
				startswith = "false";
			}
			if (startswith.equalsIgnoreCase("true")) {
				if (menuIdentifier.toLowerCase().startsWith(sec.getSectionType().toLowerCase())) {
					return sec;
				}
			} else {
				if (menuIdentifier.equalsIgnoreCase(sec.getSectionType())) {
					return sec;
				}
			}
		}
		
		return null;
	}

	public static boolean isExcludedSoft(String menuIdentifier) {
		PropertiesSection sec = getExcludedMenu(menuIdentifier);
		
		if (sec != null) {
			String s = sec.getEntryValue("mode");
			if ((s != null) && s.equalsIgnoreCase("soft")) {
				return true;
			}
		}
		
		return false;
	}

	public static boolean isExcludedFull(String menuIdentifier) {
		PropertiesSection sec = getExcludedMenu(menuIdentifier);
		
		if (sec != null) {
			String s = sec.getEntryValue("mode");
			if ((s != null) && s.equalsIgnoreCase("full")) {
				return true;
			}
		}
		
		return false;
	}

	public static List<String> getExcludedMenus() {
		List<String> l = new ArrayList<String>();
		l.addAll(excludedmenus.keySet());
		return l;
	}

	public static enum ExcludeMode {
		
		FULL("full"),
		SOFT("soft");
		
		private String s;
		
		private ExcludeMode(String s) {
			this.s = s;
		}
		
		public String getMode() {
			return this.s;
		}
	}
	
}
