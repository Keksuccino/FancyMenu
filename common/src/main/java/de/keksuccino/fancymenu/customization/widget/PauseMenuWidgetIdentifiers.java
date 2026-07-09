package de.keksuccino.fancymenu.customization.widget;

public final class PauseMenuWidgetIdentifiers {

    private PauseMenuWidgetIdentifiers() {}

    /**
     * The server-specific feedback controls replace the standalone controls on some pause-menu variants. Their
     * identifiers must follow the replaced controls so saved customizations keep working, but only when both controls
     * are not present at the same time.
     */
    public static String resolveFeedbackIdentifier(boolean hasStandaloneButton) {
        return hasStandaloneButton ? "pause_feedback_button" : "pause_send_feedback_button";
    }

    public static String resolveServerLinksIdentifier(boolean hasStandaloneButton) {
        return hasStandaloneButton ? "pause_server_links_button" : "pause_report_bugs_button";
    }

}
