package de.keksuccino.fancymenu.rendering.ui.popup;

import java.awt.Color;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.NotificationPopup;

public class FMNotificationPopup extends NotificationPopup {

	public FMNotificationPopup(int width, Color color, int backgroundAlpha, Runnable callback, String... text) {
		super(width, color, backgroundAlpha, callback, text);
	}

	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.applyDefaultButtonSkinTo(b);
	}

}
