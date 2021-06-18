package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.gui.widget.PressableWidget;

/**
 * Dummy class for button type identification
 */
public class CustomizationButton extends AdvancedButton {

	public CustomizationButton(int x, int y, int widthIn, int heightIn, String buttonText, PressAction onPress) {
		super(x, y, widthIn, heightIn, buttonText, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.setLabelShadow(false);
		UIBase.colorizeButton(this);
	}
	
	public CustomizationButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean b, PressAction onPress) {
		super(x, y, widthIn, heightIn, buttonText, b, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.enableRightclick = true;
		this.setLabelShadow(false);
		UIBase.colorizeButton(this);
	}

	public static boolean isCustomizationButton(PressableWidget w) {
		return (w instanceof CustomizationButton);
	}

}
