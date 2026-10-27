package de.keksuccino.fancymenu.mixin.support.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PauseScreenWidgetIdentifierResolver {

    private PauseScreenWidgetIdentifierResolver() {
    }

    /**
     * Resolves the stable FancyMenu identifier of a Pause screen widget by the translation key of its message.
     *
     * <p>Vanilla replaces the standalone feedback buttons with submenu and server-link variants in some pause-menu layouts.
     * The replacements must inherit the standalone identifiers unless both variants coexist, otherwise saved widget customizations stop matching.</p>
     *
     * <p>Player Reporting keeps {@code pause_share_to_lan_button}, because older pause menus showed it in place of the LAN button and saved layouts still target it.
     * The Multiplayer Options button hosts LAN publishing and coexists with Player Reporting, so it needs its own identifier to stay locatable when the menu layout shifts.</p>
     */
    @Nullable
    public static String resolve(@NotNull String translationKey, boolean hasSendFeedbackButton, boolean hasReportBugsButton) {
        return switch (translationKey) {
            case "menu.game" -> "pause_title_widget";
            case "menu.returnToGame" -> "pause_return_to_game_button";
            case "gui.advancements" -> "pause_advancements_button";
            case "gui.stats" -> "pause_stats_button";
            case "menu.feedback" -> hasSendFeedbackButton ? "pause_feedback_button" : "pause_send_feedback_button";
            case "menu.server_links" -> hasReportBugsButton ? "pause_server_links_button" : "pause_report_bugs_button";
            case "menu.options" -> "pause_options_button";
            case "menu.multiplayerOptions.button" -> "pause_multiplayer_options_button";
            case "menu.shareToLan", "menu.playerReporting" -> "pause_share_to_lan_button";
            case "menu.returnToMenu", "menu.disconnect" -> "pause_disconnect_button";
            case "menu.sendFeedback" -> "pause_send_feedback_button";
            case "menu.reportBugs" -> "pause_report_bugs_button";
            default -> null;
        };
    }

}
