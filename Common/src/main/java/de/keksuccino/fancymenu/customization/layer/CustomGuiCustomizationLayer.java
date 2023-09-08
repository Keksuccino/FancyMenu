package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiCustomizationLayer extends ScreenCustomizationLayer {

	public CustomGuiCustomizationLayer(String identifier) {
		super(identifier);
	}

	@Override
	protected boolean shouldCustomize(Screen screen) {
		if (screen instanceof CustomGuiBaseScreen) {
			if (((CustomGuiBaseScreen) screen).getIdentifier().equals(this.getIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
