package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.screen.AfterScreenRenderingEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.auth.ModValidator;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomizationOverlay {

	private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean IS_VALID_FANCYMENU_BUILD = ModValidator.isFancyMenuMetadataValid();
	private static final Map<String, ConsumingSupplier<Screen, Boolean>> OVERLAY_VISIBILITY_CONTROLLERS = new HashMap<>();

	private static CustomizationOverlayMenuBar overlayMenuBar;
	private static DebugOverlay debugOverlay;
    private static long menuBarId = -1;
    private static long debugOverlayId = -1;

    static {

        // This makes the clear version of the Pause screen not show the customization overlay
        registerOverlayVisibilityController(screen -> (screen instanceof PauseScreen p) ? p.showsPauseMenu() : true);

    }

	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new CustomizationOverlay());
	}

	public static void refreshMenuBar() {
		overlayMenuBar = CustomizationOverlayUI.buildMenuBar((overlayMenuBar == null) || overlayMenuBar.isExpanded());
        if (menuBarId != -1) {
            ScreenOverlayHandler.INSTANCE.addOverlayWithId(menuBarId, overlayMenuBar);
        } else {
            menuBarId = ScreenOverlayHandler.INSTANCE.addOverlay(overlayMenuBar);
//            ScreenOverlayHandler.INSTANCE.addVisibilityController(menuBarId, screen -> {
//                if (!isOverlayVisible(screen)) return false;
//                if (ScreenCustomization.isScreenBlacklisted(screen.getClass().getName())) return false;
//                return true;
//            });
        }
	}

	public static void refreshDebugOverlay() {
        if (debugOverlay != null) debugOverlay.resetOverlay();
		debugOverlay = CustomizationOverlayUI.buildDebugOverlay(overlayMenuBar);
        if (debugOverlayId != -1) {
            ScreenOverlayHandler.INSTANCE.addOverlayWithId(debugOverlayId, debugOverlay);
        } else {
            debugOverlayId = ScreenOverlayHandler.INSTANCE.addOverlay(debugOverlay);
//            ScreenOverlayHandler.INSTANCE.addVisibilityController(debugOverlayId, screen -> {
//                if (!isOverlayVisible(screen)) return false;
//                if (ScreenCustomization.isScreenBlacklisted(screen.getClass().getName())) return false;
//                return true;
//            });
        }
	}

	@Nullable
	public static CustomizationOverlayMenuBar getCurrentMenuBarInstance() {
		return overlayMenuBar;
	}

	@Nullable
	public static DebugOverlay getCurrentDebugOverlayInstance() {
		return debugOverlay;
	}

	public static boolean isOverlayVisible(@Nullable Screen currentScreen) {
		if (FancyMenu.getOptions().modpackMode.getValue()) return false;
		if (!FancyMenu.getOptions().showCustomizationOverlay.getValue()) return false;
		if (currentScreen == null) return false;
		for (ConsumingSupplier<Screen, Boolean> s : OVERLAY_VISIBILITY_CONTROLLERS.values()) {
			if (!s.get(currentScreen)) return false;
		}
		return true;
	}

	/**
	 * Registers a new overlay visibility controller that lets you control if the menu bar should be visible in certain screens and situations.
	 * @return The unique identifier of the controller. Useful for when you need to unregister the controller later.
	 */
	@NotNull
	public static String registerOverlayVisibilityController(@NotNull ConsumingSupplier<Screen, Boolean> visibilityController) {
		String id = ScreenCustomization.generateUniqueIdentifier();
		OVERLAY_VISIBILITY_CONTROLLERS.put(id, Objects.requireNonNull(visibilityController));
		return id;
	}

	public static void unregisterOverlayVisibilityController(@NotNull String identifier) {
		OVERLAY_VISIBILITY_CONTROLLERS.remove(Objects.requireNonNull(identifier));
	}

	@EventListener(priority = -1000)
	public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {
        refreshMenuBar();
        refreshDebugOverlay();
	}

	@EventListener
	public void onRenderPost(AfterScreenRenderingEvent e) {
		if (!IS_VALID_FANCYMENU_BUILD) {
			ModValidator.renderInvalidError(e.getGraphics());
		}
	}

	@EventListener
	public void onScreenKeyPressed(ScreenKeyPressedEvent e) {

		if (!ScreenCustomization.isScreenBlacklisted(e.getScreen().getClass().getName())) {

			String keyName = e.getKeyName();

			if (!FancyMenu.getOptions().modpackMode.getValue()) {

				//Toggle Menu Bar
				if (keyName.equals("c") && Screen.hasControlDown() && Screen.hasAltDown()) {
					FancyMenu.getOptions().showCustomizationOverlay.setValue(!FancyMenu.getOptions().showCustomizationOverlay.getValue());
					ScreenCustomization.reInitCurrentScreen();
				}

				//Toggle Debug Overlay
				if (keyName.equals("d") && Screen.hasControlDown() && Screen.hasAltDown()) {
					FancyMenu.getOptions().showDebugOverlay.setValue(!FancyMenu.getOptions().showDebugOverlay.getValue());
					ScreenCustomization.reInitCurrentScreen();
				}

				//Reload FancyMenu
				if (keyName.equals("r") && Screen.hasControlDown() && Screen.hasAltDown()) {
					ScreenCustomization.reloadFancyMenu();
				}

			}

		}

	}

}
