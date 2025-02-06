package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinPauseScreen extends Screen {

    private MixinPauseScreen(Component $$0) {
        super($$0);
    }

    /**
     * @reason Give unique identifiers to all Pause screen widgets
     */
    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    private void after_createPauseMenu_FancyMenu(CallbackInfo info) {

        this.children().forEach(guiEventListener -> {
            if (guiEventListener instanceof AbstractWidget w) {
                if (w.getMessage() instanceof TranslatableComponent c) {
                    UniqueWidget u = ((UniqueWidget)w);
                    if (c.getKey().equals("menu.game")) u.setWidgetIdentifierFancyMenu("pause_title_widget");
                    if (c.getKey().equals("menu.returnToGame")) u.setWidgetIdentifierFancyMenu("pause_return_to_game_button");
                    if (c.getKey().equals("gui.advancements")) u.setWidgetIdentifierFancyMenu("pause_advancements_button");
                    if (c.getKey().equals("gui.stats")) u.setWidgetIdentifierFancyMenu("pause_stats_button");
                    if (c.getKey().equals("menu.feedback")) u.setWidgetIdentifierFancyMenu("pause_feedback_button");
                    if (c.getKey().equals("menu.server_links")) u.setWidgetIdentifierFancyMenu("pause_server_links_button");
                    if (c.getKey().equals("menu.options")) u.setWidgetIdentifierFancyMenu("pause_options_button");
                    if (c.getKey().equals("menu.shareToLan")) u.setWidgetIdentifierFancyMenu("pause_share_to_lan_button");
                    if (c.getKey().equals("menu.playerReporting")) u.setWidgetIdentifierFancyMenu("pause_player_reporting_button");
                    if (c.getKey().equals("menu.returnToMenu")) u.setWidgetIdentifierFancyMenu("pause_return_to_menu_button");
                    if (c.getKey().equals("menu.disconnect")) u.setWidgetIdentifierFancyMenu("pause_disconnect_button");
                    if (c.getKey().equals("menu.sendFeedback")) u.setWidgetIdentifierFancyMenu("pause_send_feedback_button");
                    if (c.getKey().equals("menu.reportBugs")) u.setWidgetIdentifierFancyMenu("pause_report_bugs_button");
                }
            }
        });

    }

}
