package de.keksuccino.fancymenu.rendering.ui.popup;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;

public class FMTextInputPopup extends TextInputPopup {

	public FMTextInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha, Consumer<String> callback) {
		super(color, title, filter, backgroundAlpha, callback);
	}
	
	public FMTextInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha) {
		super(color, title, filter, backgroundAlpha);
	}
	
	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.colorizeButton(b);
	}

}
