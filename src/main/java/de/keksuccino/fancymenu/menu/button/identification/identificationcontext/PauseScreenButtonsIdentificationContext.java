package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.resources.I18n;

public class PauseScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return GuiIngameMenu.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        GuiButton b = data.getButton();
        if (b != null) {
            String msg = b.displayString;
            if (msg.equals(I18n.format("menu.returnToGame"))) {
                return "mc_pausescreen_return_to_game_button";
            }
            if (msg.equals(I18n.format("gui.advancements"))) {
                return "mc_pausescreen_advancements_button";
            }
            if (msg.equals(I18n.format("gui.stats"))) {
                return "mc_pausescreen_stats_button";
            }
            if (msg.equals(I18n.format("menu.options"))) {
                return "mc_pausescreen_options_button";
            }
            if (msg.equals(I18n.format("menu.shareToLan"))) {
                return "mc_pausescreen_lan_button";
            }
            if (msg.equals(I18n.format("menu.returnToMenu")) || msg.equals(I18n.format("menu.disconnect"))) {
                return "mc_pausescreen_disconnect_button";
            }
            if (msg.equals(I18n.format("fml.menu.modoptions"))) {
                return "forge_pausescreen_mods_button";
            }
        }
        return null;
    }

}
