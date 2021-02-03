package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerEvents;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.DummyCoreMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.LanguageMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MoreRefinedStorageMainHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.controls.ControlsMenuHandler;
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
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

@SuppressWarnings("deprecation")
public class MenuCustomization {
	
	private static PropertiesSet customizableMenus;
	
	private static boolean initDone = false;
	private static List<String> sounds = new ArrayList<String>();
	
	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");
	
	public static void init() {
		if (!initDone) {
			//Registering (base) events for the MenuCustomization system
			MinecraftForge.EVENT_BUS.register(new MenuCustomizationEvents());
			
			//Registering all custom menu handlers
			MenuHandlerRegistry.registerHandler(new MainMenuHandler());
			MenuHandlerRegistry.registerHandler(new MoreRefinedStorageMainHandler());
			MenuHandlerRegistry.registerHandler(new DummyCoreMainHandler());
			//TODO disabled for now bc useless
//			MenuHandlerRegistry.registerHandler(new WorldLoadingScreenHandler());
			
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

	public static void enableCustomizationForMenu(GuiScreen menu) {
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

	public static void disableCustomizationForMenu(GuiScreen menu) {
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

	public static boolean isMenuCustomizable(GuiScreen menu) {
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
	
	public static boolean isValidScreen(GuiScreen screen) {
		if (screen == null) {
			return false;
		}
//		if (screen instanceof NotificationModUpdateScreen) {
//			return false;
//		}
//		if (screen instanceof GuiScreenRealmsProxy) {
//			return false;
//		}
		if (Minecraft.getMinecraft().currentScreen != screen) {
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
	
	/**
	 * Will NOT return TRUE, when only values to stretch the object were used for width and/or height.
	 */
	public static boolean containsCalculations(PropertiesSet properties) {
		for (PropertiesSection s : properties.getPropertiesOfType("customization")) {
			String width = s.getEntryValue("width");
			String height = s.getEntryValue("height");
			String x = s.getEntryValue("x");
			String y = s.getEntryValue("y");
			String posX = s.getEntryValue("posX");
			String posY = s.getEntryValue("posY");
			if ((width != null) && !MathUtils.isInteger(width)) {
				if (!width.equals("%guiwidth%")) {
					return true;
				}
			}
			if ((height != null) && !MathUtils.isInteger(height)) {
				if (!height.equals("%guiheight%")) {
					return true;
				}
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
		String playername = Minecraft.getMinecraft().getSession().getUsername();
		String playeruuid = Minecraft.getMinecraft().getSession().getPlayerID();
		String mcversion = ForgeVersion.mcVersion;
		if (Minecraft.getMinecraft().currentScreen != null) {
			width = Minecraft.getMinecraft().currentScreen.width;
			height = Minecraft.getMinecraft().currentScreen.height;
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

		//Replace mod version placeholder
		s = replaceModVersionPlaceolder(s);

		//Replace loaded mods placeholder
		s = s.replace("%loadedmods%", "" + getLoadedMods());

		//Replace total mods placeholder
		s = s.replace("%totalmods%", "" + getTotalMods());
		
		return s;
	}

	private static String replaceModVersionPlaceolder(String in) {
		try {
			if (in.contains("%version:")) {
				List<String> l = new ArrayList<String>();
				int index = -1;
				int i = 0;
				while (i < in.length()) {
					String s = "" + in.charAt(i);
					if (s.equals("%")) {
						if (index == -1) {
							index = i;
						} else {
							String sub = in.substring(index, i+1);
							if (sub.startsWith("%version:") && sub.endsWith("%")) {
								l.add(sub);
							}
							index = -1;
						}
					}
					i++;
				}
				for (String s : l) {
					if (s.contains(":")) {
						String blank = s.substring(1, s.length()-1);
						String mod = blank.split(":", 2)[1];
						if (Loader.isModLoaded(mod)) {
							ModContainer c = getModContainerById(mod);
							if (c != null) {
								String version = c.getVersion();
								in = in.replace(s, version);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}
	
	private static ModContainer getModContainerById(String modid) {
		try {
			for (ModContainer c : Loader.instance().getActiveModList()) {
				if (c.getModId().equals(modid)) {
					return c;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static int getTotalMods() {
		try {
			return Loader.instance().getModList().size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int getLoadedMods() {
		try {
			return Loader.instance().getActiveModList().size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
}
