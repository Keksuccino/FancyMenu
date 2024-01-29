package de.keksuccino.fancymenu.customization;

import java.io.File;
import java.util.*;
import de.keksuccino.fancymenu.Compat;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.ElementMemories;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.LayoutEditorWidgets;
import de.keksuccino.fancymenu.customization.screen.dummyscreen.DummyScreens;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts.WidgetIdentificationContexts;
import de.keksuccino.fancymenu.customization.action.actions.Actions;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.background.backgrounds.MenuBackgrounds;
import de.keksuccino.fancymenu.customization.action.ButtonScriptHandler;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.requirements.LoadingRequirements;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.Placeholders;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIColorThemes;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ScreenCustomization {

	private static final Logger LOGGER = LogManager.getLogger();

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

		ElementMemories.init();

		ScreenCustomizationLayerHandler.init();

		DeepScreenCustomizationLayers.registerAll();

		Actions.registerAll();

		LoadingRequirements.registerAll();

		Placeholders.registerAll();

		MenuBackgrounds.registerAll();

		Elements.registerAll();

		VariableHandler.init();

		WidgetIdentificationContexts.registerAll();

		DummyScreens.registerAll();

		AnimationHandler.init();
		AnimationHandler.discoverAndRegisterExternalAnimations();

		PanoramaHandler.init();

		SlideshowHandler.init();

		CustomGuiHandler.init();

		CustomizationOverlay.init();

		LayoutHandler.init();

		readCustomizableScreensFromFile();

		ButtonScriptHandler.init();

		LastWorldHandler.init();

		LayoutEditorWidgets.registerAll();

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
		if (screen instanceof CustomGuiBaseScreen) return;
		if ((screen != null) && !isCustomizationEnabledForScreen(screen, true)) {
			//Always use the screen class path here! NEVER universal identifiers!
			String screenClassPath = screen.getClass().getName();
			PropertyContainer sec = new PropertyContainer(screenClassPath);
			customizableScreens.putContainer(sec);
			writeCustomizableScreensToFile();
		}
	}

	private static void disableCustomizationForScreen(Screen screen) {
		if (customizableScreens == null) {
			readCustomizableScreensFromFile();
		}
		if (screen instanceof CustomGuiBaseScreen) return;
		if (screen != null) {
			//Always use the screen class path here! NEVER universal identifiers!
			String screenClassPath = screen.getClass().getName();
			List<PropertyContainer> l = new ArrayList<>();
			for (PropertyContainer sec : customizableScreens.getContainers()) {
				if (!sec.getType().equals(screenClassPath)) {
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
		if (screen instanceof CustomGuiBaseScreen) {
			return true;
		}
		//Always use the screen class path here! NEVER universal identifiers!
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
		PropertiesParser.serializeSetToFile(customizableScreens, CUSTOMIZABLE_MENUS_FILE.getPath());
	}

	public static void readCustomizableScreensFromFile() {
		try {
			if (!CUSTOMIZABLE_MENUS_FILE.exists()) {
				CUSTOMIZABLE_MENUS_FILE.createNewFile();
				PropertiesParser.serializeSetToFile(new PropertyContainerSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			PropertyContainerSet s = PropertiesParser.deserializeSetFromFile(CUSTOMIZABLE_MENUS_FILE.getPath());
			if (s == null) {
				PropertiesParser.serializeSetToFile(new PropertyContainerSet("customizablemenus"), CUSTOMIZABLE_MENUS_FILE.getPath());
				s = PropertiesParser.deserializeSetFromFile(CUSTOMIZABLE_MENUS_FILE.getPath());
			}
			Objects.requireNonNull(s, "[FANCYMENU] Unable to read customizable menus file! PropertyContainer was NULL!");
			PropertyContainerSet s2 = new PropertyContainerSet("customizablemenus");
			for (PropertyContainer sec : s.getContainers()) {
				//This should never be a universal identifier, but if it is, skip it and print error log message
				String identifier = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(sec.getType());
				if (UniversalScreenIdentifierRegistry.universalIdentifierExists(identifier)) {
					LOGGER.error("[FANCYMENU] Found illegal universal identifier '" + identifier + "' in customizable menus file! Skipping..");
					continue;
				}
				if (ScreenIdentifierHandler.isValidIdentifier(identifier)) {
					s2.putContainer(new PropertyContainer(identifier));
				}
			}
			customizableScreens = s2;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TRUE when, and only when, a new TYPE of screen (different identifier than last one) gets opened.<br>
	 * Returns FALSE if an instance with the same identifier as the last one gets opened.<br><br>
	 *
	 * The isNewMenu value gets updated in an {@link InitOrResizeScreenStartingEvent},
	 * so it is already up-to-date when {@link InitOrResizeScreenEvent.Pre} and {@link InitOrResizeScreenEvent.Post} get fired.
	 */
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
		ResourceHandlers.reloadAll();
		UIColorThemes.reloadThemes();
		AnimationHandler.resetAnimations();
		AnimationHandler.resetAnimationSounds();
		AnimationHandler.stopAnimationSounds();
		LayoutHandler.reloadLayouts();
		EventHandler.INSTANCE.postEvent(new ModReloadEvent(Minecraft.getInstance().screen));
		reInitCurrentScreen();
	}

	public static void reInitCurrentScreen() {
		if (Minecraft.getInstance().screen != null) {
			RenderingUtils.resetGuiScale();
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(Minecraft.getInstance().screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			Minecraft.getInstance().screen.resize(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(Minecraft.getInstance().screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
		}
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
		addScreenBlacklistRule((screen) -> screen.startsWith("net.mehvahdjukaar.supplementaries."));
		addScreenBlacklistRule((screen) -> screen.startsWith("net.optifine"));
		addScreenBlacklistRule((screen) -> screen.startsWith("slimeknights."));
		//TODO übernehmen
		addScreenBlacklistRule((screen) -> screen.startsWith("eu.midnightdust."));
		addScreenBlacklistRule((screen) -> screen.equals(VideoSettingsScreen.class.getName()) && Compat.isOptiFineLoaded());
		addScreenBlacklistRule((screen) -> screen.startsWith("de.keksuccino.fancymenu.") && !screen.equals(CustomGuiBaseScreen.class.getName()));

	}

	@Deprecated
	public static boolean isExistingGameDirectoryPath(@NotNull String path) {
		return GameDirectoryUtils.isExistingGameDirectoryPath(path);
	}

	@Deprecated
	public static String getAbsoluteGameDirectoryPath(@NotNull String path) {
		return GameDirectoryUtils.getAbsoluteGameDirectoryPath(path);
	}

	@Deprecated
	public static String getPathWithoutGameDirectory(@NotNull String path) {
		return GameDirectoryUtils.getPathWithoutGameDirectory(path);
	}

	public static String generateUniqueIdentifier() {
		return UUID.randomUUID() + "-" + System.currentTimeMillis();
	}

	@FunctionalInterface
	public interface ScreenBlacklistRule {

		boolean isScreenBlacklisted(String screenClassPath);

	}
	
}
