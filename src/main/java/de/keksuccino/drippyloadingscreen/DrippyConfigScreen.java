package de.keksuccino.drippyloadingscreen;

import de.keksuccino.konkrete.config.ConfigEntry;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

public class DrippyConfigScreen extends ConfigScreen {

    public DrippyConfigScreen(Screen parent) {
        super(DrippyLoadingScreen.config, I18n.get("drippyloadingscreen.config"), parent);
    }

    @Override
    protected void init() {

        super.init();

        for (String s : this.config.getCategorys()) {
            this.setCategoryDisplayName(s, I18n.get("drippyloadingscreen.config.categories." + s));
        }

        for (ConfigEntry e : this.config.getAllAsEntry()) {
            this.setValueDisplayName(e.getName(), I18n.get("drippyloadingscreen.config." + e.getName()));
            this.setValueDescription(e.getName(), I18n.get("drippyloadingscreen.config." + e.getName() + ".desc"));
        }

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

}
