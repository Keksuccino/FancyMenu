package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;

public class DynamicValueInputPopup extends FMTextInputPopup {

	public DynamicValueInputPopup(Color color, String title, CharacterFilter filter, int alpha) {
		super(color, title, filter, alpha);
	}
	
	public DynamicValueInputPopup(Color color, String title, CharacterFilter filter, int backgroundAlpha, Consumer<String> callback) {
		super(color, title, filter, backgroundAlpha, callback);
	}

	@Override
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		
		super.init(color, title, filter, callback);
		
		this.textField = new DynamicValueTextfield(Minecraft.getInstance().font, 0, 0, 200, 20, true, filter);
		this.textField.setFocus(true);
		this.textField.setFocused(false);
		this.textField.setMaxLength(1000);
		
	}
	

}
