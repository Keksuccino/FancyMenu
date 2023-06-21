package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.net.URL;
import java.util.*;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.util.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.action.actions.Actions;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.background.backgrounds.MenuBackgrounds;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.widget.VanillaButtonHandler;
import de.keksuccino.fancymenu.customization.widget.identification.ButtonIdentificator;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.LoadingRequirements;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.Placeholders;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingHandler;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenCustomization {

	public static final File CUSTOMIZABLE_MENUS_FILE = new File("config/fancymenu/customizablemenus.txt");

	private static final List<ScreenBlacklistRule> SCREEN_BLACKLIST_RULES = new ArrayList<>();

	private static PropertyContainerSet customizableScreens;
	protected static boolean isCurrentScrollable = false;
	protected static boolean isNewMenu = true;
	protected static ScreenCustomizationEvents eventsInstance = new ScreenCustomizationEvents();
	protected static boolean screenCustomizationEnabled = true;

	private static boolean initialized = false;
	
	public static void init() {

		if (initialized) {
			return;
		}

		EventHandler.INSTANCE.registerListenersOf(eventsInstance);

		addDefaultScreenBlacklistRules();

		ScreenCustomizationLayerHandler.init();

		VanillaButtonHandler.init();

		DeepScreenCustomizationLayers.registerAll();

		Actions.registerAll();

		LoadingRequirements.registerAll();

		Placeholders.registerAll();

		MenuBackgrounds.registerAll();

		Elements.registerAll();

		VariableHandler.init();

		ButtonIdentificator.init();

		AnimationHandler.init();
		AnimationHandler.discoverAndRegisterExternalAnimations();

		PanoramaHandler.init();

		SlideshowHandler.init();

		CustomGuiLoader.loadCustomGuis();

		GameIntroHandler.init();

		CustomizationOverlay.init();

		LayoutHandler.init();

		readCustomizableScreensFromFile();

		ActionExecutor.init();

		LastWorldHandler.init();

		initialized = true;

	}

	public static boolean isScreenCustomizationEnabled() {
		return screenCustomizationEnabled;
	}

	public static void setScreenCustomizationEnabled(boolean enabled) {
		ScreenCustomization.screenCustomizationEnabled = enabled;
	}

	public static void setCustomizationForScreenEnabled(Screen screen, boolean enabled) {
		if (enabled) {
			enableCustomizationForScreen(screen);
		} else {
			disableCustomizationForScreen(screen);
		}
	}

	private static void enableCustomizationForScreen(Screen screen) {
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if ((screen != null) && !isCustomizationEnabledForScreen(screen, true)) {
			if (!(screen instanceof CustomGuiBase)) {
				String identifier = screen.getClass().getName();
				PropertyContainer sec = new PropertyContainer(identifier);
				customizableScreens.putContainer(sec);
				writeCustomizableScreensToFile();
			}
		}
	}

	private static void disableCustomizationForScreen(Screen screen) {
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
		return isCustomizationEnabledForScreen(screen, false);
	}

	public static boolean isCustomizationEnabledForScreen(Screen screen, boolean ignoreAllowScreenCustomization) {
		if (screen == null) return false;
		if (isScreenBlacklisted(screen)) {
			return false;
		}
		if (!screenCustomizationEnabled && !ignoreAllowScreenCustomization) {
			return false;
		}
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if (screen instanceof CustomGuiBase) {
			return true;
		}
		List<PropertyContainer> s = customizableScreens.getContainersOfType(screen.getClass().getName());
		return !s.isEmpty();
	}

	public static void disableCustomizationForAllScreens() {
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		customizableScreens.getContainers().clear();
		writeCustomizableScreensToFile();
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
			Objects.requireNonNull(s, "[FANCYMENU] Unable to read customizable menus file! PropertyContainer was NULL!");
			PropertyContainerSet s2 = new PropertyContainerSet("customizablemenus");
			for (PropertyContainer sec : s.getContainers()) {
				String identifier = null;
				try {
					if (sec.getType().length() > 5) {
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
				ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(s);
				if (layer != null) layer.resetLayer();
				reInitCurrentScreen();
			}
		}
	}

	public static void reloadFancyMenu() {
		FancyMenu.reloadOptions();
		TextureHandler.INSTANCE.clearResources();
		SoundRegistry.resetSounds();
		SoundRegistry.stopSounds();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		LayoutHandler.reloadLayouts();
		CustomGuiLoader.loadCustomGuis();
		EventHandler.INSTANCE.postEvent(new ModReloadEvent(Minecraft.getInstance().screen));
		reInitCurrentScreen();
	}

	public static void reInitCurrentScreen() {
		if (Minecraft.getInstance().screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(Minecraft.getInstance().screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen));
			Minecraft.getInstance().screen.resize(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(Minecraft.getInstance().screen));
		}
	}

	@Nullable
	public static String getScreenIdentifier(Screen screen) {
		if (screen == null) return null;
		if (screen instanceof CustomGuiBase c) {
			return c.getIdentifier();
		}
		return screen.getClass().getName();
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

	public static boolean isScreenBlacklisted(@NotNull Screen screen) {
		return isScreenBlacklisted(screen.getClass().getName());
	}

	public static boolean isScreenBlacklisted(@NotNull String screenClassPath) {
		for (ScreenBlacklistRule rule : SCREEN_BLACKLIST_RULES) {
			if (rule.isScreenBlacklisted(screenClassPath)) return true;
		}
		return false;
	}

	private static void addDefaultScreenBlacklistRules() {

		addScreenBlacklistRule((screen) -> screen.startsWith("com.simibubi.create."));
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.panoramica."));
		addScreenBlacklistRule((screen) -> screen.startsWith("com.github.alexthe666.alexsmobs."));
		addScreenBlacklistRule((screen) -> screen.startsWith("io.github.lgatodu47.screenshot_viewer."));
		addScreenBlacklistRule((screen) -> screen.startsWith("twilightforest."));
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.spiffyhud."));
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.drippyloadingscreen."));
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.fmaudio."));
		addScreenBlacklistRule((screen) -> screen.startsWith("net.mehvahdjukaar.supplementaries."));
		addScreenBlacklistRule((screen) -> screen.startsWith("net.optifine"));
		addScreenBlacklistRule((screen) -> screen.startsWith("slimeknights."));
		addScreenBlacklistRule((screen) -> screen.equals(VideoSettingsScreen.class.getName()) && FancyMenu.isOptiFineLoaded());
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.fancymenu.") && !screen.equals(CustomGuiBase.class.getName()));

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

	public static String getAbsoluteGameDirectoryPath(@NotNull String path) {
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

	public static String getPathWithoutGameDirectory(@NotNull String path) {
		Objects.requireNonNull(path, "Path cannot be NULL!");
		File f = new File(getAbsoluteGameDirectoryPath(path));
		String p = f.getAbsolutePath().replace(FancyMenu.getGameDirectory().getAbsolutePath(), "");
		if (p.startsWith("/")) p = p.substring(1);
		return p;
	}

	public static String generateUniqueIdentifier() {
		long ms = System.currentTimeMillis();
		String s = UUID.randomUUID().toString();
		return s + ms;
	}

	@FunctionalInterface
	public interface ScreenBlacklistRule {

		boolean isScreenBlacklisted(String screenClassPath);

	}
	
}
