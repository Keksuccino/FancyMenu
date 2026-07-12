package de.keksuccino.fancymenu.mixin.support.client;

import javax.annotation.Nullable;

public final class PauseScreenWidgetIdentifierResolver {

    private PauseScreenWidgetIdentifierResolver() {
    }

    /**
     * Vanilla replaces the standalone feedback buttons with submenu and server-link variants in some pause-menu layouts.
     * The replacements must inherit the standalone identifiers unless both variants coexist, otherwise saved widget customizations stop matching.
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
            case "menu.shareToLan", "menu.playerReporting" -> "pause_share_to_lan_button";
            case "menu.returnToMenu", "menu.disconnect" -> "pause_disconnect_button";
            case "menu.sendFeedback" -> "pause_send_feedback_button";
            case "menu.reportBugs" -> "pause_report_bugs_button";
            default -> null;
        };
    }

}
