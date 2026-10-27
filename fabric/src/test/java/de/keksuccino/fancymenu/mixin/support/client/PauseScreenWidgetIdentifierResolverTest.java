package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PauseScreenWidgetIdentifierResolverTest {

    @Test
    void worldOptionsButtonHasDedicatedIdentifier() {
        assertEquals("pause_world_options_button", PauseScreenWidgetIdentifierResolver.resolve("options.worldOptions.button", true, true));
        assertEquals("pause_world_options_button", PauseScreenWidgetIdentifierResolver.resolve("options.worldOptions.button", false, false));
    }

    @Test
    void worldOptionsButtonDoesNotShareCoexistingPlayerReportingIdentifier() {
        String worldOptionsIdentifier = PauseScreenWidgetIdentifierResolver.resolve("options.worldOptions.button", true, true);
        String playerReportingIdentifier = PauseScreenWidgetIdentifierResolver.resolve("menu.playerReporting", true, true);

        assertNotEquals(playerReportingIdentifier, worldOptionsIdentifier);
    }

    @Test
    void playerReportingKeepsLegacyLanIdentifier() {
        assertEquals("pause_share_to_lan_button", PauseScreenWidgetIdentifierResolver.resolve("menu.playerReporting", true, true));
        assertEquals("pause_share_to_lan_button", PauseScreenWidgetIdentifierResolver.resolve("menu.shareToLan", true, true));
    }

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
    void unchangedVanillaIdentifiersStayStable() {
        assertEquals("pause_title_widget", PauseScreenWidgetIdentifierResolver.resolve("menu.game", false, false));
        assertEquals("pause_return_to_game_button", PauseScreenWidgetIdentifierResolver.resolve("menu.returnToGame", false, false));
        assertEquals("pause_advancements_button", PauseScreenWidgetIdentifierResolver.resolve("gui.advancements", false, false));
        assertEquals("pause_stats_button", PauseScreenWidgetIdentifierResolver.resolve("gui.stats", false, false));
        assertEquals("pause_options_button", PauseScreenWidgetIdentifierResolver.resolve("menu.options", false, false));
        assertEquals("pause_disconnect_button", PauseScreenWidgetIdentifierResolver.resolve("menu.returnToMenu", false, false));
        assertEquals("pause_disconnect_button", PauseScreenWidgetIdentifierResolver.resolve("menu.disconnect", false, false));
    }

    @Test
    void unknownTranslationKeyHasNoIdentifier() {
        assertNull(PauseScreenWidgetIdentifierResolver.resolve("menu.custom_options", false, false));
    }

}
