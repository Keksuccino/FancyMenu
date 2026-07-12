package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PauseScreenWidgetIdentifierResolverTest {

    @ParameterizedTest
    @CsvSource({
            "menu.feedback, false, false, pause_send_feedback_button",
            "menu.feedback, true, false, pause_feedback_button",
            "menu.feedback, true, true, pause_feedback_button",
            "menu.server_links, false, false, pause_report_bugs_button",
            "menu.server_links, false, true, pause_server_links_button",
            "menu.server_links, true, true, pause_server_links_button"
    })
    void resolvesServerReplacementIdentityBasedOnCorrespondingStandaloneButton(String translationKey, boolean hasSendFeedbackButton, boolean hasReportBugsButton, String expectedIdentifier) {
        assertEquals(expectedIdentifier, PauseScreenWidgetIdentifierResolver.resolveReplacementWidgetIdentifier(translationKey, hasSendFeedbackButton, hasReportBugsButton));
    }

    @Test
    void ignoresUnrelatedWidgets() {
        assertNull(PauseScreenWidgetIdentifierResolver.resolveReplacementWidgetIdentifier("menu.options", false, false));
    }

}
