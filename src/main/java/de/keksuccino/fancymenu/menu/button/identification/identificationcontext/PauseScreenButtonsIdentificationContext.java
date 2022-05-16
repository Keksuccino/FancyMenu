package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class PauseScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return PauseScreen.class;
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
                if (key.equals("menu.returnToGame")) {
                    return "mc_pausescreen_return_to_game_button";
                }
                if (key.equals("gui.advancements")) {
                    return "mc_pausescreen_advancements_button";
                }
                if (key.equals("gui.stats")) {
                    return "mc_pausescreen_stats_button";
                }
                if (key.equals("menu.sendFeedback")) {
                    return "mc_pausescreen_feedback_button";
                }
                if (key.equals("menu.reportBugs")) {
                    return "mc_pausescreen_report_bugs_button";
                }
                if (key.equals("menu.options")) {
                    return "mc_pausescreen_options_button";
                }
                if (key.equals("menu.shareToLan")) {
                    return "mc_pausescreen_lan_button";
                }
                if (key.equals("menu.returnToMenu") || key.equals("menu.disconnect")) {
                    return "mc_pausescreen_disconnect_button";
                }
            }
        }
        return null;
    }

}
