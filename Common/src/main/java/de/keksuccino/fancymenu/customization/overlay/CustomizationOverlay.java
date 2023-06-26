package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.screen.KeyPressedScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class CustomizationOverlay {

	private static MenuBar overlayMenuBar;
	
	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new CustomizationOverlay());
	}

	public static void rebuildMenuBar() {
		overlayMenuBar = CustomizationOverlayUI.buildMenuBar();
	}

	@Nullable
	public static MenuBar getCurrentMenuBarInstance() {
		return overlayMenuBar;
	}

	@EventListener(priority = -1000)
	public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {
		rebuildMenuBar();
		e.getWidgets().add(0, overlayMenuBar);
	}

	@EventListener(priority = EventPriority.LOW)
	public void onRenderPost(RenderScreenEvent.Post e) {
		if (!ScreenCustomization.isScreenBlacklisted(e.getScreen().getClass().getName()) && (overlayMenuBar != null)) {
			if (FancyMenu.getOptions().showCustomizationOverlay.getValue()) {
				overlayMenuBar.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
			}
		}
	}

	@EventListener
	public void onScreenKeyPressed(KeyPressedScreenEvent e) {

		String keyName = e.getKeyName();

		//Toggle Menu Bar
		if (keyName.equals("c") && Screen.hasControlDown() && Screen.hasAltDown()) {
			FancyMenu.getOptions().showCustomizationOverlay.setValue(!FancyMenu.getOptions().showCustomizationOverlay.getValue());
			ScreenCustomization.reInitCurrentScreen();
		}

		//TODO add CTRL + ALT + R --> Reload FancyMenu

	}

}
