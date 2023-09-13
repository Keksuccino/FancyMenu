package de.keksuccino.fancymenu.customization.layer;

import java.util.HashMap;
import java.util.Map;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenCustomizationLayerHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<String, ScreenCustomizationLayer> LAYERS = new HashMap<>();

	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new ScreenCustomizationLayerHandler());
	}

	private ScreenCustomizationLayerHandler() {
	}

	public static void registerScreen(@NotNull Screen screen) {
		String identifier = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
		if (!LAYERS.containsKey(identifier)) {
			ScreenCustomizationLayer layer = new ScreenCustomizationLayer(identifier);
			registerLayer(identifier, layer);
		}
	}

	public static void registerLayer(@NotNull ScreenCustomizationLayer layer) {
		registerLayer(layer.getScreenIdentifier(), layer);
	}

	public static void registerLayer(@NotNull String screenIdentifier, @NotNull ScreenCustomizationLayer layer) {
		if (!LAYERS.containsKey(screenIdentifier)) {
			LOGGER.info("[FANCYMENU] ScreenCustomizationLayer registered: " + screenIdentifier);
		} else {
			LOGGER.warn("[FANCYMENU] ScreenCustomizationLayer replaced: " + screenIdentifier);
		}
		LAYERS.put(screenIdentifier, layer);
	}
	
	public static boolean isLayerRegistered(@NotNull String screenIdentifier) {
		return LAYERS.containsKey(screenIdentifier);
	}

	@Nullable
	public static ScreenCustomizationLayer getActiveLayer() {
		Screen s = Minecraft.getInstance().screen;
		if ((s != null) && !ScreenCustomization.isScreenBlacklisted(s)) {
			return getLayerOfScreen(s);
		}
		return null;
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
	public static ScreenCustomizationLayer getLayer(@NotNull String screenIdentifier) {
		return LAYERS.get(screenIdentifier);
	}

	// Event Handling ------------------->

	@EventListener
	public void onScreenInitOrResizeStarting(InitOrResizeScreenStartingEvent e) {
		this.autoRegisterScreenLayer(e.getScreen());
	}

	protected void autoRegisterScreenLayer(Screen screen) {
		if (screen != null) {
			if (ScreenCustomization.isScreenBlacklisted(screen)) {
				return;
			}
			if (screen instanceof CustomGuiBaseScreen c) {
				if (!isLayerRegistered(c.getIdentifier())) {
					registerLayer(new ScreenCustomizationLayer(c.getIdentifier()));
				}
			} else {
				registerScreen(screen);
			}
		}
	}

}
