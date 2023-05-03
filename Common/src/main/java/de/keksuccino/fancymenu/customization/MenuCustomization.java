package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.net.URL;
import java.util.*;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.backend.LayoutHandler;
import de.keksuccino.fancymenu.customization.backend.action.actions.Actions;
import de.keksuccino.fancymenu.customization.backend.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.backend.button.ButtonCache;
import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.customization.backend.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.DeepCustomizationLayers;
import de.keksuccino.fancymenu.customization.backend.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.backend.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.backend.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.CustomizationItems;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.playerentity.PlayerEntityRotationScreen;
import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.requirements.LoadingRequirements;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.backend.layer.layers.*;
import de.keksuccino.fancymenu.customization.backend.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.backend.placeholder.v1.placeholders.Placeholders;
import de.keksuccino.fancymenu.customization.backend.setupsharing.SetupSharingEngine;
import de.keksuccino.fancymenu.customization.backend.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.backend.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.backend.world.LastWorldHandler;
import de.keksuccino.fancymenu.customization.frontend.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.frontend.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.fancymenu.rendering.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MenuCustomization {

	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");

	private static final List<String> REGISTERED_SOUNDS = new ArrayList<>();

	private static PropertiesSet customizableMenus;
	protected static boolean isCurrentScrollable = false;
	protected static boolean isNewMenu = true;
	protected static MenuCustomizationEvents eventsInstance = new MenuCustomizationEvents();
	public static boolean allowScreenCustomization = false;

	private static boolean initialized = false;
	
	public static void init() {

		if (initialized) {
			return;
		}

		EventHandler.INSTANCE.registerListenersOf(eventsInstance);

		ScreenCustomizationLayerHandler.init();
		ScreenCustomizationLayers.registerAll();

		DeepCustomizationLayers.registerAll();

		Actions.registerAll();

		LoadingRequirements.registerAll();

		Placeholders.registerAll();

		de.keksuccino.fancymenu.customization.backend.placeholder.v2.placeholders.Placeholders.registerAll();

		CustomizationItems.registerAll();

		VariableHandler.init();

		ButtonIdentificator.init();

		AnimationHandler.init();
		AnimationHandler.loadCustomAnimations();

		PanoramaHandler.init();

		SlideshowHandler.init();

		CustomGuiLoader.loadCustomGuis();

		GameIntroHandler.init();

		CustomizationOverlay.init();

		ButtonCache.init();

		LayoutHandler.init();

		updateCustomizableMenuCache();

		ButtonScriptEngine.init();

		LastWorldHandler.init();

		initialized = true;

	}

	public static void updateCustomizableMenuCache() {
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
				} catch (Exception ignored) {}
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
				if (customizableMenus != null) {
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
				if (customizableMenus != null) {
					List<PropertiesSection> l = new ArrayList<>();
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
		if (menu != null) {
			if (menu instanceof CustomGuiBase) {
				return true;
			}
			String identifier = menu.getClass().getName();
			if (customizableMenus != null) {
				List<PropertiesSection> s = customizableMenus.getPropertiesOfType(identifier);
				return (s != null) && !s.isEmpty();
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
		} catch (Exception ignored) {}
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
		return Minecraft.getInstance().screen == screen;
	}
	
	public static void registerSound(String key, String path) {
		if (!REGISTERED_SOUNDS.contains(key)) {
			REGISTERED_SOUNDS.add(key);
		}
		SoundHandler.registerSound(key, path);
	}
	
	public static void unregisterSound(String key) {
		REGISTERED_SOUNDS.remove(key);
		SoundHandler.unregisterSound(key);
	}
	
	public static void stopSounds() {
		for (String s : REGISTERED_SOUNDS) {
			SoundHandler.stopSound(s);
		}
	}
	
	public static void resetSounds() {
		for (String s : REGISTERED_SOUNDS) {
			SoundHandler.resetSound(s);
		}
	}

	public static boolean isSoundRegistered(String key) {
		return REGISTERED_SOUNDS.contains(key);
	}

	public static List<String> getSounds() {
		return REGISTERED_SOUNDS;
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
		ExternalTextureHandler.INSTANCE.clearResources();
		resetSounds();
		stopSounds();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		LayoutHandler.reloadLayouts();
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

	public static boolean isScreenOverridden(Screen current) {
		if ((current instanceof CustomGuiBase) && (((CustomGuiBase) current).getOverriddenScreen() != null)) {
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
		if (menuIdentifier.startsWith("de.keksuccino.fancymenu.customization.frontend.layouteditor.loadingrequirements.")) {
			return true;
		}
		if (menuIdentifier.equals(ConfirmationScreen.class.getName())) {
			return true;
		}
		if (menuIdentifier.startsWith("de.keksuccino.fancymenu.customization.frontend.layouteditor.actions.")) {
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
				return gameDir + "/" + path;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return path;
	}
	
}
