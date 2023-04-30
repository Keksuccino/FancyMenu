package de.keksuccino.fancymenu.customization.customizationgui;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import net.minecraft.client.Minecraft;

public class PlaceholderInputPopup extends FMTextInputPopup {

	public PlaceholderInputPopup(Color color, String title, CharacterFilter filter, int alpha) {
		super(color, title, filter, alpha);
	}
	
	public PlaceholderInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha, Consumer<String> callback) {
		super(color, title, filter, backgroundAlpha, callback);
	}

	@Override
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		
		super.init(color, title, filter, callback);
		
		this.textField = new PlaceholderEditBox(Minecraft.getInstance().font, 0, 0, 200, 20, true, filter);
		this.textField.setCanLoseFocus(true);
		this.textField.setFocused(false);
		this.textField.setMaxLength(1000);
		
	}
	

}
