//---
package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class DeathScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return DeathScreen.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        AbstractWidget b = data.getButton();
        if (b != null) {
            Component c = b.getMessage();
            if (c instanceof TranslatableComponent) {
                String key = ((TranslatableComponent)c).getKey();
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
