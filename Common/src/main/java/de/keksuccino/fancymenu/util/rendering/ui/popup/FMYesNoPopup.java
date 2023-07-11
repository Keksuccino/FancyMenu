package de.keksuccino.fancymenu.util.rendering.ui.popup;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;

//TODO remove this class
@Deprecated
public class FMYesNoPopup extends YesNoPopup {

	public FMYesNoPopup(int width, Color color, int backgroundAlpha, Consumer<Boolean> callback, String... text) {
		super(width, color, backgroundAlpha, callback, text);
	}
	
	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.applyDefaultWidgetSkinTo(b);
	}

}
