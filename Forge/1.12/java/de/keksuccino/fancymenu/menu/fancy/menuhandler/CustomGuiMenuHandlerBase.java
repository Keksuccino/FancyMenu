package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import net.minecraft.client.gui.GuiScreen;

public class CustomGuiMenuHandlerBase  extends MenuHandlerBase {

	public CustomGuiMenuHandlerBase(String identifier) {
		super(identifier);
	}

	@Override
	protected boolean shouldCustomize(GuiScreen menu) {
		if (menu instanceof CustomGuiBase) {
			if (((CustomGuiBase) menu).getIdentifier().equals(this.getMenuIdentifier())) {
				return true;
			}
		}
		return false;
	}

}
