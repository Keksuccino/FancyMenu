package de.keksuccino.fancymenu.customization.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PauseMenuWidgetIdentifiersTest {

    @Test
    void feedbackReplacementInheritsStandaloneIdentifier() {
        assertEquals("pause_send_feedback_button", PauseMenuWidgetIdentifiers.resolveFeedbackIdentifier(false));
    }

    @Test
    void feedbackControlKeepsDistinctIdentifierAlongsideStandaloneControl() {
        assertEquals("pause_feedback_button", PauseMenuWidgetIdentifiers.resolveFeedbackIdentifier(true));
    }

    @Test
    void serverLinksReplacementInheritsStandaloneIdentifier() {
        assertEquals("pause_report_bugs_button", PauseMenuWidgetIdentifiers.resolveServerLinksIdentifier(false));
    }

    @Test
    void serverLinksControlKeepsDistinctIdentifierAlongsideStandaloneControl() {
        assertEquals("pause_server_links_button", PauseMenuWidgetIdentifiers.resolveServerLinksIdentifier(true));
    }

}
