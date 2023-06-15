package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.rendering.ui.menubar.v2.MenuBar;

public class CustomizationOverlay {

	private static MenuBar overlayMenuBar;
	
	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new CustomizationOverlay());
	}

	public static void rebuildMenuBar() {
		overlayMenuBar = CustomizationOverlayUI.buildMenuBar();
	}

	@EventListener(priority = -1000)
	public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {
		rebuildMenuBar();
		e.getWidgets().add(0, overlayMenuBar);
	}

	@EventListener(priority = EventPriority.LOW)
	public void onRenderPost(RenderScreenEvent.Post e) {
		if (!ScreenCustomization.isScreenBlacklisted(e.getScreen().getClass().getName()) && (overlayMenuBar != null)) {
			overlayMenuBar.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}
	}

}
