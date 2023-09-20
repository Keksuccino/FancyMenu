package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts;

import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class PauseScreenWidgetIdentificationContext extends WidgetIdentificationContext {

    public PauseScreenWidgetIdentificationContext() {

        this.addUniversalIdentifierProvider(meta -> {
            String key = meta.getWidgetLocalizationKey();
            if (key != null) {
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
            return null;
        });

    }

    @Override
    public @NotNull Class<? extends Screen> getTargetScreen() {
        return PauseScreen.class;
    }

}
