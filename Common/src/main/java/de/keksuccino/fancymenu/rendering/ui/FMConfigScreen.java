package de.keksuccino.fancymenu.rendering.ui;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.config.ConfigEntry;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.gui.screens.Screen;

public class FMConfigScreen extends ConfigScreen {

	public FMConfigScreen(Screen parent) {
		super(FancyMenu.getConfig(), Locals.localize("FancyMenu.getConfig()"), parent);
	}
	
	@Override
	protected void init() {
		super.init();
		
		for (String s : this.config.getCategorys()) {
			this.setCategoryDisplayName(s, Locals.localize("FancyMenu.getConfig().categories." + s));
		}
		
		for (ConfigEntry e : this.config.getAllAsEntry()) {
			this.setValueDisplayName(e.getName(), Locals.localize("FancyMenu.getConfig()." + e.getName()));
			this.setValueDescription(e.getName(), Locals.localize("FancyMenu.getConfig()." + e.getName() + ".desc"));
		}
		
	}
	
}
