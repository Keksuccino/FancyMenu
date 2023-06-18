package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.config.ConfigEntry;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;

public class FMConfigScreen extends ConfigScreen {

	public FMConfigScreen(Screen parent) {
		super(FancyMenu.getConfig(), I18n.get("fancymenu.config"), parent);
	}
	
	@Override
	protected void init() {
		super.init();
		
		for (String s : this.config.getCategorys()) {
			this.setCategoryDisplayName(s, I18n.get("fancymenu.config.categories." + s));
		}
		
		for (ConfigEntry e : this.config.getAllAsEntry()) {
			this.setValueDisplayName(e.getName(), I18n.get("fancymenu.config." + e.getName()));
			this.setValueDescription(e.getName(), I18n.get("fancymenu.config." + e.getName() + ".desc").replace("\n", "%n%"));
		}
		
	}
	
}
