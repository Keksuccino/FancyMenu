package de.keksuccino.fancymenu.customization.backend.menuhandler;

import de.keksuccino.fancymenu.customization.backend.guicreator.CustomGuiBase;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiMenuHandlerBase  extends MenuHandlerBase {

	public CustomGuiMenuHandlerBase(String identifier) {
		super(identifier);
	}

	@Override
	protected boolean shouldCustomize(Screen menu) {
		if (menu instanceof CustomGuiBase) {
			if (((CustomGuiBase) menu).getIdentifier().equals(this.getMenuIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
