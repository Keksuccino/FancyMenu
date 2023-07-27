package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiBase;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiCustomizationLayer extends ScreenCustomizationLayer {

	public CustomGuiCustomizationLayer(String identifier) {
		super(identifier);
	}

	@Override
	protected boolean shouldCustomize(Screen screen) {
		if (screen instanceof CustomGuiBase) {
			if (((CustomGuiBase) screen).getIdentifier().equals(this.getIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
