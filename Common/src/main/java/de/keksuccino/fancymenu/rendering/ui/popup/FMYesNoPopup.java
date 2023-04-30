package de.keksuccino.fancymenu.rendering.ui.popup;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;

public class FMYesNoPopup extends YesNoPopup {

	public FMYesNoPopup(int width, Color color, int backgroundAlpha, Consumer<Boolean> callback, String... text) {
		super(width, color, backgroundAlpha, callback, text);
	}
	
	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.colorizeButton(b);
	}

}
