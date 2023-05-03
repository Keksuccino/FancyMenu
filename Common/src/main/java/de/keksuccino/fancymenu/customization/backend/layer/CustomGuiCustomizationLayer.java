package de.keksuccino.fancymenu.customization.backend.layer;

import de.keksuccino.fancymenu.customization.backend.guicreator.CustomGuiBase;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiCustomizationLayer extends ScreenCustomizationLayer {

	public CustomGuiCustomizationLayer(String identifier) {
		super(identifier);
	}

	@Override
	protected boolean shouldCustomize(Screen menu) {
		if (menu instanceof CustomGuiBase) {
			if (((CustomGuiBase) menu).getIdentifier().equals(this.getIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
