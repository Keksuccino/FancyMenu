package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.config.ConfigEntry;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.gui.GuiScreen;

public class FMConfigScreen extends ConfigScreen {

	public FMConfigScreen(GuiScreen parent) {
		super(FancyMenu.config, Locals.localize("fancymenu.config"), parent);
	}
	
	@Override
	public void initGui() {
		
		super.initGui();
		
		for (String s : this.config.getCategorys()) {
			this.setCategoryDisplayName(s, Locals.localize("fancymenu.config.categories." + s));
		}
		
		for (ConfigEntry e : this.config.getAllAsEntry()) {
			this.setValueDisplayName(e.getName(), Locals.localize("fancymenu.config." + e.getName()));
			this.setValueDescription(e.getName(), Locals.localize("fancymenu.config." + e.getName() + ".desc"));
		}
		
	}
	
}
