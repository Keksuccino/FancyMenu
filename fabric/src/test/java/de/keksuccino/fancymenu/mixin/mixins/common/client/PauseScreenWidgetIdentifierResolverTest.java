package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PauseScreenWidgetIdentifierResolverTest {

    @Test
    void directLayoutUsesStandaloneIdentifiers() {
        assertEquals("pause_send_feedback_button", PauseScreenWidgetIdentifierResolver.resolve("menu.sendFeedback", true, true));
        assertEquals("pause_report_bugs_button", PauseScreenWidgetIdentifierResolver.resolve("menu.reportBugs", true, true));
    }

    @Test
    void replacementLayoutInheritsStandaloneIdentifiers() {
        assertEquals("pause_send_feedback_button", PauseScreenWidgetIdentifierResolver.resolve("menu.feedback", false, false));
        assertEquals("pause_report_bugs_button", PauseScreenWidgetIdentifierResolver.resolve("menu.server_links", false, false));
    }

    @Test
    void coexistingVariantsKeepDistinctIdentifiers() {
        assertEquals("pause_feedback_button", PauseScreenWidgetIdentifierResolver.resolve("menu.feedback", true, true));
        assertEquals("pause_server_links_button", PauseScreenWidgetIdentifierResolver.resolve("menu.server_links", true, true));
    }

    @Test
    void partiallyCoexistingVariantsResolveIndependently() {
        assertEquals("pause_feedback_button", PauseScreenWidgetIdentifierResolver.resolve("menu.feedback", true, false));
        assertEquals("pause_report_bugs_button", PauseScreenWidgetIdentifierResolver.resolve("menu.server_links", true, false));
        assertEquals("pause_send_feedback_button", PauseScreenWidgetIdentifierResolver.resolve("menu.feedback", false, true));
        assertEquals("pause_server_links_button", PauseScreenWidgetIdentifierResolver.resolve("menu.server_links", false, true));
    }

    @Test
    void unknownTranslationKeyHasNoIdentifier() {
        assertNull(PauseScreenWidgetIdentifierResolver.resolve("menu.custom_options", false, false));
    }

}
