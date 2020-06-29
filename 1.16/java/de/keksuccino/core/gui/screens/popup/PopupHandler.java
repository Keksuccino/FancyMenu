package de.keksuccino.core.gui.screens.popup;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PopupHandler {
	
	private static Popup popup;
	private static boolean initDone = false;
	
	public static void init() {
		if (!initDone) {
			MinecraftForge.EVENT_BUS.register(PopupHandler.class);
			initDone = true;
		}
	}
	
	@SubscribeEvent
	public static void onMouseInput(GuiScreenEvent.MouseClickedEvent.Pre e) {
		if ((popup != null) && popup.isDisplayed()) {
			e.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public static void onRender(GuiScreenEvent.DrawScreenEvent.Post e) {
		if ((popup != null) && popup.isDisplayed()) {
			popup.render(e.getMatrixStack(), e.getMouseX(), e.getMouseY(), e.getGui());
		}
	}
	
	public static boolean isPopupActive() {
		if (popup == null) {
			return false;
		}
		return popup.isDisplayed();
	}
	
	public static Popup getCurrentPopup() {
		return popup;
	}
	
	public static void displayPopup(Popup p) {
		popup = p;
		popup.setDisplayed(true);
	}

}
