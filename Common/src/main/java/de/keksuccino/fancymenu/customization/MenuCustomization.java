package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.PreloadedLayoutEditorScreen;
import de.keksuccino.fancymenu.customization.customizationgui.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.customization.menuhandler.custom.*;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.customizationgui.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingEngine;
import de.keksuccino.fancymenu.rendering.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.customization.item.v2.items.playerentity.PlayerEntityRotationScreen;
import de.keksuccino.fancymenu.customization.menuhandler.MenuHandlerEvents;
import de.keksuccino.fancymenu.customization.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ITextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MenuCustomization {

	private static PropertiesSet customizableMenus;
	
	private static boolean initDone = false;
	private static List<String> sounds = new ArrayList<String>();

	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");

	protected static boolean isCurrentScrollable = false;
	protected static boolean isNewMenu = true;
	protected static MenuCustomizationEvents eventsInstance = new MenuCustomizationEvents();
	
	public static boolean allowScreenCustomization = false;
	
	public static void init() {
		if (!initDone) {
			//Registering (base) events for the MenuCustomization system
			EventHandler.INSTANCE.registerListenersOf(eventsInstance);
			
			//Registering all custom menu handlers
			MenuHandlerRegistry.registerHandler(new MainMenuHandler());
			MenuHandlerRegistry.registerHandler(new MoreRefinedStorageMainHandler());
			MenuHandlerRegistry.registerHandler(new DummyCoreMainHandler());
			MenuHandlerRegistry.registerHandler(new WorldLoadingScreenHandler());
			MenuHandlerRegistry.registerHandler(new PauseScreenHandler());
			
			//Registering event to automatically register handlers for all menus (its necessary to do this AFTER registering custom handlers!)
			EventHandler.INSTANCE.registerListenersOf(new MenuHandlerEvents());
			
			CustomizationOverlay.init();
			
			//Registering the update event for the button cache
			EventHandler.INSTANCE.registerListenersOf(new ButtonCache());
			
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
						Class.forName(sec.getSectionType(), false, MenuCustomization.class.getClassLoader());
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
		
		if (!allowScreenCustomization) {
			return false;
		}
		//------------------------
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
			Class.forName(identifier, false, MenuCustomization.class.getClassLoader());
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

	public static void reloadCurrentScreen() {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			if (isMenuCustomizable(s)) {
				setIsNewMenu(true);
				SoftMenuReloadEvent e = new SoftMenuReloadEvent(s);
				EventHandler.INSTANCE.postEvent(e);
				Minecraft.getInstance().setScreen(s);
			}
		}
	}

	public static void reloadFancyMenu() {
		FancyMenu.updateConfig();
		clearKonkreteTextureCache();
		resetSounds();
		stopSounds();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		reloadLayouts();
		MenuHandlerRegistry.setActiveHandler(null);
		CustomGuiLoader.loadCustomGuis();
		if (!FancyMenu.getConfig().getOrDefault("showcustomizationbuttons", true)) {
			CustomizationOverlayUI.showButtonInfo = false;
			CustomizationOverlayUI.showMenuInfo = false;
		}
		EventHandler.INSTANCE.postEvent(new MenuReloadEvent(Minecraft.getInstance().screen));
		try {
			Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void reloadLayouts() {
		if (initDone) {
			updateCustomizeableMenuCache();
			//Resets itself automatically and can be used for both loading and reloading
			MenuCustomizationProperties.loadProperties();
		}
	}

	public static void clearKonkreteTextureCache() {
		try {
			Field texturesField = TextureHandler.class.getDeclaredField("textures");
			texturesField.setAccessible(true);
			Map<String, ITextureResourceLocation> textures = (Map<String, ITextureResourceLocation>) texturesField.get(TextureHandler.class);
			textures.clear();
			Field gifsField = TextureHandler.class.getDeclaredField("gifs");
			gifsField.setAccessible(true);
			Map<String, ExternalGifAnimationRenderer> gifs = (Map<String, ExternalGifAnimationRenderer>) gifsField.get(TextureHandler.class);
			for (ExternalGifAnimationRenderer g : gifs.values()) {
				g.setLooped(false);
				g.resetAnimation();
			}
			gifs.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void enableLayout(String path) {
		try {
			File f = new File(path);
			String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationsDirectory().getPath(), Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(FancyMenu.getCustomizationsDirectory().getPath() + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		reloadFancyMenu();
	}

	public static void enableLayout(MenuCustomizationProperties.LayoutProperties layout) {
		if (layout.path != null) {
			enableLayout(layout.path);
		}
	}

	public static void disableLayout(String path) {
		try {
			File f = new File(path);
			String disPath = FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled";
			String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(path), "txt");
			FileUtils.copyFile(f, new File(disPath + "/" + name));
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		reloadFancyMenu();
	}

	public static void disableLayout(MenuCustomizationProperties.LayoutProperties layout) {
		if (layout.path != null) {
			disableLayout(layout.path);
		}
	}

	public static void editLayout(Screen current, File layout) {
		try {
			if ((layout != null) && (current != null) && (layout.exists()) && (layout.isFile())) {
				List<PropertiesSet> l = new ArrayList<>();
				PropertiesSet set = PropertiesSerializer.getProperties(layout.getPath());
				l.add(set);
				List<PropertiesSection> meta = set.getPropertiesOfType("customization-meta");
				if (meta.isEmpty()) {
					meta = set.getPropertiesOfType("type-meta");
				}
				if (!meta.isEmpty()) {
					meta.get(0).addEntry("path", layout.getPath());
					LayoutEditorScreen.isActive = true;
					Minecraft.getInstance().setScreen(new PreloadedLayoutEditorScreen(current, l));
					MenuCustomization.stopSounds();
					MenuCustomization.resetSounds();
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							((AdvancedAnimation)r).stopAudio();
							if (((AdvancedAnimation)r).replayIntro()) {
								r.resetAnimation();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Will save the layout as layout file.
	 *
	 * @param to Full file path with file name + extension.
	 */
	public static boolean saveLayoutTo(PropertiesSet layout, String to) {
		File f = new File(to);
		String s = Files.getFileExtension(to);
		if ((s != null) && !s.equals("")) {
			if (f.exists() && f.isFile()) {
				f.delete();
			}
			PropertiesSerializer.writeProperties(layout, f.getPath());
			return true;
		}
		return false;
	}

	/**
	 * Will save the layout as layout file.
	 *
	 * @param to Full file path with file name + extension.
	 */
	public static boolean saveLayoutTo(List<PropertiesSection> layout, String to) {
		PropertiesSet props = new PropertiesSet("menu");
		for (PropertiesSection sec : layout) {
			props.addProperties(sec);
		}
		return saveLayoutTo(props, to);
	}

	public static boolean isScreenOverridden(Screen current) {
		if ((current != null) && (current instanceof CustomGuiBase) && (((CustomGuiBase)current).getOverriddenScreen() != null)) {
			return true;
		}
		return false;
	}

	public static boolean isBlacklistedMenu(String menuIdentifier) {
		if (menuIdentifier.startsWith(PlayerEntityRotationScreen.class.getName())) {
			return true;
		}
		if (menuIdentifier.startsWith("com.simibubi.create.")) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.panoramica.")) {
			return true;
		}
		if (menuIdentifier.startsWith("com.github.alexthe666.alexsmobs.")) {
			return true;
		}
		if (menuIdentifier.equals(TextEditorScreen.class.getName())) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.fancymenu.customization.customizationgui.layouteditor.loadingrequirements.")) {
			return true;
		}
		if (menuIdentifier.equals(ConfirmationScreen.class.getName())) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.fancymenu.customization.customizationgui.layouteditor.actions.")) {
			return true;
		}
		if (menuIdentifier.startsWith("io.github.lgatodu47.screenshot_viewer.")) {
			return true;
		}
		if (menuIdentifier.startsWith("twilightforest.")) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.spiffyhud.")) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.drippyloadingscreen.")) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.fmaudio.")) {
			return true;
		}
		return false;
	}

	public static void openFile(File f) {
		try {
			String url = f.toURI().toURL().toString();
			String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			URL u = new URL(url);
			if (!Minecraft.ON_OSX) {
				if (s.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					if (u.getProtocol().equals("file")) {
						url = url.replace("file:", "file://");
					}
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			} else {
				Runtime.getRuntime().exec(new String[]{"open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
