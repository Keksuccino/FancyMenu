package de.keksuccino.fancymenu.customization.widget.identification;

import javax.annotation.Nullable;

public final class PauseScreenWidgetIdentifierResolver {

    private PauseScreenWidgetIdentifierResolver() {}

    public static @Nullable String resolveReplacementWidgetIdentifier(String translationKey, boolean hasSendFeedbackButton, boolean hasReportBugsButton) {
        if (translationKey.equals("menu.feedback")) return hasSendFeedbackButton ? "pause_feedback_button" : "pause_send_feedback_button";
        if (translationKey.equals("menu.server_links")) return hasReportBugsButton ? "pause_server_links_button" : "pause_report_bugs_button";
        return null;
    }

}
