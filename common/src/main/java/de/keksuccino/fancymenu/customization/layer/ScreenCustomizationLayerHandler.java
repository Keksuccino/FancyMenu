package de.keksuccino.fancymenu.customization.layer;

import java.util.HashMap;
import java.util.Map;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenCustomizationLayerHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<String, ScreenCustomizationLayer> LAYERS = new HashMap<>();

	private static volatile boolean neverReloaded = true;
	private static volatile boolean resourceReload = false;

	private ScreenCustomizationLayerHandler() {
	}

	public static void init() {

		EventHandler.INSTANCE.registerListenersOf(new ScreenCustomizationLayerHandler());

		MinecraftResourceReloadObserver.addReloadListener(ScreenCustomizationLayerHandler::onMinecraftReload);

	}

	/**
	 * Gets called before Minecraft starts and after it finished a resource reload via {@link LoadingOverlay}.<br>
	 * Some stuff like firing screen init and render events still get injected into {@link LoadingOverlay} via mixins.
	 */
	private static void onMinecraftReload(@NotNull MinecraftResourceReloadObserver.ReloadAction reloadAction) {

		if (reloadAction == MinecraftResourceReloadObserver.ReloadAction.STARTING) {

			LOGGER.info("[FANCYMENU] Minecraft resource reload: STARTING");

			resourceReload = true;

		} else { //FINISHED

			neverReloaded = false;
			resourceReload = false;

			//Reset isNewMenu, so first-time stuff and on-load stuff works correctly
			ScreenCustomization.setIsNewMenu(true);

			LOGGER.info("[FANCYMENU] Minecraft resource reload: FINISHED");

		}

	}

	/**
	 * Returns if the initial resource reload ({@link LoadingOverlay} when starting the game) is finished.
	 */
	public static boolean isBeforeFinishInitialMinecraftReload() {
		return neverReloaded;
	}

	/**
	 * If Minecraft is currently reloading its resources (via {@link LoadingOverlay}).
	 */
	public static boolean isMinecraftCurrentlyReloading() {
		return resourceReload;
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
		screenIdentifier = ScreenIdentifierHandler.getBestIdentifier(screenIdentifier);
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
		if (screen instanceof CustomGuiBaseScreen custom) {
			return getLayer(custom.getIdentifier());
		}
		return getLayerOfScreen(screen.getClass());
	}

	@Nullable
	public static ScreenCustomizationLayer getLayerOfScreen(@NotNull Class<? extends Screen> screenClass) {
		if (screenClass == CustomGuiBaseScreen.class) throw new IllegalArgumentException("Tried to get ScreenCustomizationLayer of CustomGuiBaseScreen class! This is not possible!");
		return getLayer(screenClass.getName());
	}

	@Nullable
	public static ScreenCustomizationLayer getLayer(@NotNull String screenIdentifier) {
		return LAYERS.get(ScreenIdentifierHandler.getBestIdentifier(screenIdentifier));
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
