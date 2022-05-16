package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.resources.I18n;

public class DeathScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return GuiGameOver.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        GuiButton b = data.getButton();
        if (b != null) {
            String msg = b.displayString;
            if (msg.equals(I18n.format("deathScreen.spectate")) || msg.equals(I18n.format("deathScreen.respawn"))) {
                return "mc_deathscreen_respawn_button";
            }
            if (msg.equals(I18n.format("deathScreen.titleScreen")) || msg.equals(I18n.format("deathScreen.deleteWorld")) || msg.equals(I18n.format("deathScreen.leaveServer"))) {
                return "mc_deathscreen_titlemenu_button";
            }
        }
        return null;
    }

}
