package de.keksuccino.fancymenu.customization.backend.button.identification.identificationcontext;

import de.keksuccino.fancymenu.customization.backend.button.ButtonData;
import de.keksuccino.fancymenu.customization.backend.button.identification.ButtonIdentificator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;

public class DeathScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class<?> getMenu() {
        return DeathScreen.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        AbstractWidget b = data.getButton();
        if (b != null) {
            String key = ButtonIdentificator.getLocalizationKeyForButton(b);
            if (key != null) {
                if (key.equals("deathScreen.spectate") || key.equals("deathScreen.respawn")) {
                    return "mc_deathscreen_respawn_button";
                }
                if (key.equals("deathScreen.titleScreen")) {
                    return "mc_deathscreen_titlemenu_button";
                }
            }
        }
        return null;
    }

}
