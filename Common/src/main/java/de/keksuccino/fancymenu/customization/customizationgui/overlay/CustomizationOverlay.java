package de.keksuccino.fancymenu.customization.customizationgui.overlay;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.SubscribeEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.MenuCustomization;

public class CustomizationOverlay {
	
	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new CustomizationOverlay());
		CustomizationOverlayUI.init();
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onRenderPost(RenderScreenEvent.Post e) {
		if (!MenuCustomization.isBlacklistedMenu(e.getScreen().getClass().getName())) {
			CustomizationOverlayUI.render(e.getPoseStack(), e.getScreen());
		}
	}
	
	public static void updateUI() {
		CustomizationOverlayUI.updateUI();
	}

}
