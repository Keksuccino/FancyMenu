package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.gui.widget.Widget;

/**
 * Dummy class for button type identification
 */
public class CustomizationButton extends AdvancedButton {

	public CustomizationButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(x, y, widthIn, heightIn, buttonText, onPress);
		this.ignoreBlockedInput = true;
		this.setBackgroundColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
	}
	
	public CustomizationButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean b, IPressable onPress) {
		super(x, y, widthIn, heightIn, buttonText, b, onPress);
		this.ignoreBlockedInput = true;
		this.setBackgroundColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
	}

	public static boolean isCustomizationButton(Widget w) {
		return (w instanceof CustomizationButton);
	}

}
