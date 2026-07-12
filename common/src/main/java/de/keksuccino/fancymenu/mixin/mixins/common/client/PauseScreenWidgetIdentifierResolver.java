package de.keksuccino.fancymenu.mixin.mixins.common.client;

import javax.annotation.Nullable;

final class PauseScreenWidgetIdentifierResolver {

    private PauseScreenWidgetIdentifierResolver() {}

    static @Nullable String resolveReplacementWidgetIdentifier(String translationKey, boolean hasSendFeedbackButton, boolean hasReportBugsButton) {
        if (translationKey.equals("menu.feedback")) return hasSendFeedbackButton ? "pause_feedback_button" : "pause_send_feedback_button";
        if (translationKey.equals("menu.server_links")) return hasReportBugsButton ? "pause_server_links_button" : "pause_report_bugs_button";
        return null;
    }

}
