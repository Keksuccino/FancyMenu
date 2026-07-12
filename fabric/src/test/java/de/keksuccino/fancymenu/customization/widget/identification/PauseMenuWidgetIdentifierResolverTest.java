package de.keksuccino.fancymenu.customization.widget.identification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PauseMenuWidgetIdentifierResolverTest {

    @Test
    void keepsStandaloneFeedbackIdentifiersUsedByMinecraft1201() {
        assertEquals("pause_send_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.sendFeedback", true, true));
        assertEquals("pause_report_bugs_button", PauseMenuWidgetIdentifierResolver.resolve("menu.reportBugs", true, true));
    }

    @Test
    void serverVariantsInheritReplacedStandaloneIdentifiers() {
        assertEquals("pause_send_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.feedback", false, false));
        assertEquals("pause_report_bugs_button", PauseMenuWidgetIdentifierResolver.resolve("menu.server_links", false, false));
    }

    @Test
    void coexistingVariantsRetainDistinctIdentifiers() {
        assertEquals("pause_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.feedback", true, true));
        assertEquals("pause_server_links_button", PauseMenuWidgetIdentifierResolver.resolve("menu.server_links", true, true));
        assertEquals("pause_send_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.sendFeedback", true, true));
        assertEquals("pause_report_bugs_button", PauseMenuWidgetIdentifierResolver.resolve("menu.reportBugs", true, true));
    }

    @Test
    void evaluatesFeedbackAndReportLayoutsIndependently() {
        assertEquals("pause_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.feedback", true, false));
        assertEquals("pause_report_bugs_button", PauseMenuWidgetIdentifierResolver.resolve("menu.server_links", true, false));
        assertEquals("pause_send_feedback_button", PauseMenuWidgetIdentifierResolver.resolve("menu.feedback", false, true));
        assertEquals("pause_server_links_button", PauseMenuWidgetIdentifierResolver.resolve("menu.server_links", false, true));
    }

    @Test
    void leavesUnknownWidgetsUnidentified() {
        assertNull(PauseMenuWidgetIdentifierResolver.resolve("modded.unknown", false, false));
    }

}
