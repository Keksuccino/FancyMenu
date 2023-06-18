package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.network.chat.Component;

//TODO delete this class

@Deprecated
public class OverlayButton extends AdvancedButton {

	@Deprecated
	public OverlayButton(int x, int y, int width, int height, String label, boolean handleSelf, OnPress onPress) {
		super(x, y, width, height, label, handleSelf, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.enableRightclick = true;
		this.setLabelShadow(false);
		UIBase.applyDefaultButtonSkinTo(this);
	}

	@Deprecated
	public OverlayButton(int x, int y, int width, int height, String label, OnPress onPress) {
		this(x, y, width, height, label, false, onPress);
	}

	@Deprecated
	public OverlayButton(int x, int y, int width, int height, Component label, boolean handleSelf, OnPress onPress) {
		super(x, y, width, height, label.getString(), handleSelf, onPress);
		this.ignoreBlockedInput = true;
		this.ignoreLeftMouseDownClickBlock = true;
		this.enableRightclick = true;
		this.setLabelShadow(false);
		UIBase.applyDefaultButtonSkinTo(this);
	}

	@Deprecated
	public OverlayButton(int x, int y, int width, int height, Component label, OnPress onPress) {
		this(x, y, width, height, label, false, onPress);
	}

}
