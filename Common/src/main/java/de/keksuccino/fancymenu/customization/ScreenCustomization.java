package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.net.URL;
import java.util.*;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.action.actions.Actions;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.background.backgrounds.MenuBackgrounds;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.button.VanillaButtonHandler;
import de.keksuccino.fancymenu.customization.button.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityRotationScreen;
import de.keksuccino.fancymenu.customization.layer.layers.ScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.LoadingRequirements;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.Placeholders;
import de.keksuccino.fancymenu.customization.placeholder.v1.placeholders.Placeholders;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingHandler;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.event.events.ScreenReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenCustomization {

	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");

	private static final List<ScreenBlacklistRule> SCREEN_BLACKLIST_RULES = new ArrayList<>();

	private static PropertyContainerSet customizableScreens;
	protected static boolean isCurrentScrollable = false;
	protected static boolean isNewMenu = true;
	protected static ScreenCustomizationEvents eventsInstance = new ScreenCustomizationEvents();
	public static boolean allowScreenCustomization = false;

	private static boolean initialized = false;
	
	public static void init() {

		if (initialized) {
			return;
		}

		EventHandler.INSTANCE.registerListenersOf(eventsInstance);

		addDefaultScreenBlacklistRules();

		ScreenCustomizationLayerHandler.init();
		ScreenCustomizationLayers.registerAll();

		VanillaButtonHandler.init();

		DeepScreenCustomizationLayers.registerAll();

		Actions.registerAll();

		LoadingRequirements.registerAll();

		Placeholders.registerAll();

		Placeholders.registerAll();

		MenuBackgrounds.registerAll();

		Elements.registerAll();

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

		readCustomizableScreensFromFile();

		ActionExecutor.init();

		LastWorldHandler.init();

		initialized = true;

	}

	public static void enableCustomizationForScreen(Screen screen) {
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if (screen != null) {
			if (!(screen instanceof CustomGuiBase)) {
				String identifier = screen.getClass().getName();
				PropertyContainer sec = new PropertyContainer(identifier);
				customizableScreens.putContainer(sec);
				writeCustomizableScreensToFile();
			}
		}
	}

	public static void disableCustomizationForScreen(Screen screen) {
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if (screen != null) {
			if (!(screen instanceof CustomGuiBase)) {
				String identifier = screen.getClass().getName();
				List<PropertyContainer> l = new ArrayList<>();
				for (PropertyContainer sec : customizableScreens.getContainers()) {
					if (!sec.getType().equals(identifier)) {
						l.add(sec);
					}
				}
				customizableScreens = new PropertyContainerSet("customizablemenus");
				for (PropertyContainer sec : l) {
					customizableScreens.putContainer(sec);
				}
				writeCustomizableScreensToFile();
			}
		}
	}

	public static boolean isCustomizationEnabledForScreen(Screen screen) {
		if (!allowScreenCustomization) {
			return false;
		}
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if (screen != null) {
			if (screen instanceof CustomGuiBase) {
				return true;
			}
			String identifier = screen.getClass().getName();
			List<PropertyContainer> s = customizableScreens.getSectionsOfType(identifier);
			return (s != null) && !s.isEmpty();
		}
		return false;
	}

	private static void writeCustomizableScreensToFile() {
		PropertiesSerializer.serializePropertyContainerSet(customizableScreens, CUSTOMIZABLE_MENUS_FILE.getPath());
	}

	public static void readCustomizableScreensFromFile() {
		try {
			if (!CUSTOMIZABLE_MENUS_FILE.exists()) {
				CUSTOMIZABLE_MENUS_FILE.createNewFile();
				PropertiesSerializer.serializePropertyContainerSet(new PropertyContainerSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			PropertyContainerSet s = PropertiesSerializer.deserializePropertyContainerSet(CUSTOMIZABLE_MENUS_FILE.getPath());
			if (s == null) {
				PropertiesSerializer.serializePropertyContainerSet(new PropertyContainerSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
				s = PropertiesSerializer.deserializePropertyContainerSet(CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			PropertyContainerSet s2 = new PropertyContainerSet("customizablemenus");
			for (PropertyContainer sec : s.getContainers()) {
				String identifier = null;
				try {
					if ((sec.getType() != null) && (sec.getType().length() > 5)) {
						Class.forName(sec.getType(), false, ScreenCustomization.class.getClassLoader());
						identifier = sec.getType();
					}
				} catch (Exception ignored) {}
				if (identifier == null) {
					identifier = findValidMenuIdentifierFor(sec.getType());
				}
				s2.putContainer(new PropertyContainer(identifier));
			}
			customizableScreens = s2;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String findValidMenuIdentifierFor(String identifier) {
		if (CustomGuiLoader.guiExists(identifier)) {
			return identifier;
		}
		SetupSharingHandler.MenuIdentifierDatabase db = SetupSharingHandler.getIdentifierDatabase();
		try {
			Class.forName(identifier, false, ScreenCustomization.class.getClassLoader());
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

	public static boolean isNewMenu() {
		return isNewMenu;
	}

	public static void setIsNewMenu(boolean b) {
		isNewMenu = b;
		eventsInstance.lastScreen = null;
	}

	public static boolean isCurrentMenuScrollable() {
		return isCurrentScrollable;
	}

	public static void reloadCurrentScreen() {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			if (isCustomizationEnabledForScreen(s)) {
				setIsNewMenu(true);
				ScreenReloadEvent e = new ScreenReloadEvent(s);
				EventHandler.INSTANCE.postEvent(e);
				Minecraft.getInstance().setScreen(s);
			}
		}
	}

	public static void reloadFancyMenu() {
		FancyMenu.updateConfig();
		TextureHandler.INSTANCE.clearResources();
		SoundRegistry.resetSounds();
		SoundRegistry.stopSounds();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		LayoutHandler.reloadLayouts();
		CustomGuiLoader.loadCustomGuis();
		if (!FancyMenu.getConfig().getOrDefault("showcustomizationbuttons", true)) {
			CustomizationOverlayUI.showButtonInfo = false;
			CustomizationOverlayUI.showMenuInfo = false;
		}
		EventHandler.INSTANCE.postEvent(new ModReloadEvent(Minecraft.getInstance().screen));
		try {
			Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isOverridingOtherScreen(Screen current) {
		return (current instanceof CustomGuiBase) && (((CustomGuiBase)current).getOverriddenScreen() != null);
	}

	public static void addScreenBlacklistRule(ScreenBlacklistRule rule) {
		SCREEN_BLACKLIST_RULES.add(rule);
	}

	public static List<ScreenBlacklistRule> getScreenBlacklistRules() {
		return new ArrayList<>(SCREEN_BLACKLIST_RULES);
	}

	public static boolean isScreenBlacklisted(Screen screen) {
		for (ScreenBlacklistRule rule : SCREEN_BLACKLIST_RULES) {
			if (rule.isScreenBlacklisted(screen)) return true;
		}
		return false;
	}

	private static void addDefaultScreenBlacklistRules() {

		addScreenBlacklistRule((screen) -> screen instanceof PlayerEntityRotationScreen);
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("com.simibubi.create."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.panoramica."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("com.github.alexthe666.alexsmobs."));
		addScreenBlacklistRule((screen) -> screen instanceof TextEditorScreen);
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.fancymenu.customization.layouteditor.loadingrequirements."));
		addScreenBlacklistRule((screen) -> screen instanceof ConfirmationScreen);
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.fancymenu.customization.layouteditor.actions."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("io.github.lgatodu47.screenshot_viewer."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("twilightforest."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.spiffyhud."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.drippyloadingscreen."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("de.keksuccino.fmaudio."));
		addScreenBlacklistRule((screen) -> screen.getClass().getName().startsWith("net.mehvahdjukaar.supplementaries."));

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

	public static String generateUniqueIdentifier() {
		long ms = System.currentTimeMillis();
		String s = UUID.randomUUID().toString();
		return s + ms;
	}

	@FunctionalInterface
	public interface ScreenBlacklistRule {

		boolean isScreenBlacklisted(Screen screen);

	}
	
}
