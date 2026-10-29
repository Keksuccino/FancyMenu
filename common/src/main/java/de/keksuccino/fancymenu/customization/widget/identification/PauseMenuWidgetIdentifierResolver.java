package de.keksuccino.fancymenu.customization.widget.identification;

import javax.annotation.Nullable;

public final class PauseMenuWidgetIdentifierResolver {

    private PauseMenuWidgetIdentifierResolver() {
    }

    /**
     * Resolves stable pause-menu widget identifiers across Minecraft's standalone and server-specific feedback layouts.
     * The server variants replace the standalone controls in older multiplayer menus, so they must inherit the saved
     * standalone identifiers unless both variants coexist in the same menu.
     * Player Reporting takes the LAN button's place outside of unpublished singleplayer worlds, but has its own identifier,
     * so both can be customized separately. {@link WidgetIdentifierMigration} upgrades layouts that still use the shared one.
     */
    @Nullable
    public static String resolve(String translationKey, boolean hasSendFeedbackButton, boolean hasReportBugsButton) {
        return switch (translationKey) {
            case "menu.game" -> "pause_title_widget";
            case "menu.returnToGame" -> "pause_return_to_game_button";
            case "gui.advancements" -> "pause_advancements_button";
            case "gui.stats" -> "pause_stats_button";
            case "menu.feedback" -> hasSendFeedbackButton ? "pause_feedback_button" : "pause_send_feedback_button";
            case "menu.server_links" -> hasReportBugsButton ? "pause_server_links_button" : "pause_report_bugs_button";
            case "menu.options" -> "pause_options_button";
            case "menu.shareToLan" -> "pause_share_to_lan_button";
            case "menu.playerReporting" -> "pause_player_reporting_button";
            case "menu.returnToMenu", "menu.disconnect" -> "pause_disconnect_button";
            case "menu.sendFeedback" -> "pause_send_feedback_button";
            case "menu.reportBugs" -> "pause_report_bugs_button";
            default -> null;
        };
    }

}
