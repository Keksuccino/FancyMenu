package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.fancymenu.menu.button.AdvancedButton;
import net.minecraft.client.gui.GuiButton;

/**
 * Dummy class for button type identification
 */
public class CustomizationButton extends AdvancedButton {

	public CustomizationButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(x, y, widthIn, heightIn, buttonText, onPress);
	}

	public static boolean isCustomizationButton(GuiButton w) {
		return (w instanceof CustomizationButton);
	}

}
