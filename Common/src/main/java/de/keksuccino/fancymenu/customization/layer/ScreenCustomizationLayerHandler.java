package de.keksuccino.fancymenu.customization.layer;

import java.util.HashMap;
import java.util.Map;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenStartingEvent;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenCustomizationLayerHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<String, ScreenCustomizationLayer> LAYERS = new HashMap<>();
	protected static ScreenCustomizationLayer activeLayer;

	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new ScreenCustomizationLayerHandler());
	}

	protected ScreenCustomizationLayerHandler() {
	}

	public static void registerScreen(@NotNull Screen screen) {
		registerScreen(screen.getClass());
	}

	public static void registerScreen(@NotNull Class<? extends Screen> screenClass) {
		if (!LAYERS.containsKey(screenClass.getName())) {
			ScreenCustomizationLayer layer = new ScreenCustomizationLayer(screenClass.getName());
			registerLayer(screenClass.getName(), layer);
		}
	}

	public static void registerLayer(@NotNull ScreenCustomizationLayer layer) {
		if (layer.getIdentifier() == null) {
			LOGGER.error("[FANCYMENU] Unable to register ScreenCustomizationLayer! Menu identifier was NULL!");
			return;
		}
		registerLayer(layer.getIdentifier(), layer);
	}

	public static void registerLayer(@NotNull String identifier, @NotNull ScreenCustomizationLayer layer) {
		if (!LAYERS.containsKey(identifier)) {
			LOGGER.info("[FANCYMENU] ScreenCustomizationLayer registered: " + identifier);
		} else {
			LOGGER.info("[FANCYMENU] ScreenCustomizationLayer replaced: " + identifier);
		}
		LAYERS.put(identifier, layer);
	}
	
	public static boolean isLayerRegistered(@NotNull String identifier) {
		return LAYERS.containsKey(identifier);
	}
	
	public static ScreenCustomizationLayer getActiveLayer() {
		return activeLayer;
	}

	public static void setActiveLayerByIdentifier(@NotNull String identifier) {
		try {
			ScreenCustomizationLayer layer = LAYERS.get(identifier);
			if (layer != null) {
				activeLayer = layer;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setActiveLayer(@Nullable ScreenCustomizationLayer layer) {
		activeLayer = layer;
	}

	@Nullable
	public static ScreenCustomizationLayer getLayerOfScreen(@NotNull Screen screen) {
		return getLayerOfScreen(screen.getClass());
	}

	@Nullable
	public static ScreenCustomizationLayer getLayerOfScreen(@NotNull Class<? extends Screen> screenClass) {
		return getLayer(screenClass.getName());
	}

	@Nullable
	public static ScreenCustomizationLayer getLayer(@NotNull String identifier) {
		return LAYERS.get(identifier);
	}

	// Event Handling ------------------->

	@EventListener
	public void onScreenInitOrResizeStarting(InitOrResizeScreenStartingEvent e) {
		this.autoRegisterScreenLayer(e.getScreen());
		this.autoSetActiveLayer(e.getScreen());
	}

	protected void autoRegisterScreenLayer(Screen screen) {
		if (screen != null) {
			if (ScreenCustomization.isScreenBlacklisted(screen.getClass().getName())) {
				return;
			}
			if (screen instanceof CustomGuiBase) {
				if (!isLayerRegistered(((CustomGuiBase)screen).getIdentifier())) {
					registerLayer(((CustomGuiBase) screen).getIdentifier(), new CustomGuiCustomizationLayer(((CustomGuiBase) screen).getIdentifier()));
				}
			} else {
				registerScreen(screen);
			}
		}
	}

	protected void autoSetActiveLayer(Screen screen) {
		if (screen != null) {
			ScreenCustomizationLayer layer;
			if (screen instanceof CustomGuiBase) {
				layer = getLayer(((CustomGuiBase) screen).getIdentifier());
			} else {
				layer = getLayerOfScreen(screen);
			}
			setActiveLayer(layer);
		}
	}

}
