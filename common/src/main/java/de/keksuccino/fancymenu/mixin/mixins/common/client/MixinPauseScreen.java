package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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

        boolean hasSendFeedbackButton = false;
        boolean hasReportBugsButton = false;
        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener instanceof AbstractWidget widget && widget.getMessage().getContents() instanceof TranslatableContents contents) {
                if (contents.getKey().equals("menu.sendFeedback")) hasSendFeedbackButton = true;
                if (contents.getKey().equals("menu.reportBugs")) hasReportBugsButton = true;
            }
        }

        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener instanceof AbstractWidget w) {
                if (w.getMessage().getContents() instanceof TranslatableContents c) {
                    UniqueWidget u = ((UniqueWidget)w);
                    if (c.getKey().equals("menu.game")) u.setWidgetIdentifierFancyMenu("pause_title_widget");
                    if (c.getKey().equals("menu.returnToGame")) u.setWidgetIdentifierFancyMenu("pause_return_to_game_button");
                    if (c.getKey().equals("gui.advancements")) u.setWidgetIdentifierFancyMenu("pause_advancements_button");
                    if (c.getKey().equals("gui.stats")) u.setWidgetIdentifierFancyMenu("pause_stats_button");
                    // Minecraft 1.21.1 replaces the two standalone feedback buttons with these two server variants. Reuse the replaced widget IDs only for that mutually-exclusive layout; newer menus can contain both sets and need distinct IDs.
                    String replacementIdentifier = PauseScreenWidgetIdentifierResolver.resolveReplacementWidgetIdentifier(c.getKey(), hasSendFeedbackButton, hasReportBugsButton);
                    if (replacementIdentifier != null) u.setWidgetIdentifierFancyMenu(replacementIdentifier);
                    if (c.getKey().equals("menu.options")) u.setWidgetIdentifierFancyMenu("pause_options_button");
                    if (c.getKey().equals("menu.shareToLan")) u.setWidgetIdentifierFancyMenu("pause_share_to_lan_button");
                    if (c.getKey().equals("menu.playerReporting")) u.setWidgetIdentifierFancyMenu("pause_share_to_lan_button");
                    if (c.getKey().equals("menu.returnToMenu")) u.setWidgetIdentifierFancyMenu("pause_disconnect_button");
                    if (c.getKey().equals("menu.disconnect")) u.setWidgetIdentifierFancyMenu("pause_disconnect_button");
                    if (c.getKey().equals("menu.sendFeedback")) u.setWidgetIdentifierFancyMenu("pause_send_feedback_button");
                    if (c.getKey().equals("menu.reportBugs")) u.setWidgetIdentifierFancyMenu("pause_report_bugs_button");
                }
            }
        }

    }

}
