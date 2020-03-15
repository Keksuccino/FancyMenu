package de.keksuccino.fancymenu.menu.fancy.helper;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;

/**
 * Dummy class for button type identification
 */
public class CustomizationButton extends Button {

	public CustomizationButton(int widthIn, int heightIn, int p_i51141_3_, int p_i51141_4_, String text, IPressable onPress) {
		super(widthIn, heightIn, p_i51141_3_, p_i51141_4_, text, onPress);
	}

	public static boolean isCustomizationButton(Widget w) {
		return (w instanceof CustomizationButton);
	}
}
