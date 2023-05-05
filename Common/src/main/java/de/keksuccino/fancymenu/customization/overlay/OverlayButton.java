package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;

public class OverlayButton extends AdvancedButton {

	public OverlayButton(int x, int y, int widthIn, int heightIn, String buttonText, OnPress onPress) {
		super(x, y, widthIn, heightIn, buttonText, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.enableRightclick = true;
		this.setLabelShadow(false);
		UIBase.applyDefaultButtonSkinTo(this);
	}
	
	public OverlayButton(int x, int y, int widthIn, int heightIn, String buttonText, boolean b, OnPress onPress) {
		super(x, y, widthIn, heightIn, buttonText, b, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.enableRightclick = true;
		this.setLabelShadow(false);
		UIBase.applyDefaultButtonSkinTo(this);
	}

}
