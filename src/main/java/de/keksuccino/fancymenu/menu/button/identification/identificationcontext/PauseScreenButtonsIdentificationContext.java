package de.keksuccino.fancymenu.menu.button.identification.identificationcontext;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PauseScreenButtonsIdentificationContext extends MenuButtonsIdentificationContext {

    @Override
    public Class getMenu() {
        return IngameMenuScreen.class;
    }

    @Override
    protected String getRawCompatibilityIdentifierForButton(ButtonData data) {
        if (data.getScreen().getClass() != this.getMenu()) {
            return null;
        }
        Widget b = data.getButton();
        if (b != null) {
            ITextComponent c = b.getMessage();
            if (c instanceof TranslationTextComponent) {
                String key = ((TranslationTextComponent)c).getKey();
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
