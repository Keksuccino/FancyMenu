package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class WidgetIdentifierMigrationTest {

    private static final String LAN_IDENTIFIER = "pause_share_to_lan_button";
    private static final String REPORTING_IDENTIFIER = "pause_player_reporting_button";

    @ParameterizedTest
    @ValueSource(strings = {LAN_IDENTIFIER, "vanillabtn:" + LAN_IDENTIFIER, "button_compatibility_id:" + LAN_IDENTIFIER})
    void unversionedLayoutGivesPlayerReportingACopyOfTheSharedCustomization(String storedIdentifier) {
        SerializedElement lan = entry("instance_identifier", storedIdentifier);
        lan.putProperty("is_hidden", "true");
        lan.putProperty("label", "Line 1\nLine 2");
        List<SerializedElement> entries = new ArrayList<>(List.of(lan));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION);

        assertEquals(2, entries.size());
        assertSame(lan, entries.get(0));
        assertEquals(storedIdentifier, lan.getValue("instance_identifier"));
        SerializedElement reporting = entries.get(1);
        assertNotSame(lan, reporting);
        assertEquals(lan.getType(), reporting.getType());
        assertEquals(REPORTING_IDENTIFIER, WidgetIdentifierHandler.getTargetWidgetIdentifier(reporting));
        Map<String, String> expectedProperties = new LinkedHashMap<>(lan.getProperties());
        expectedProperties.put("instance_identifier", REPORTING_IDENTIFIER);
        assertEquals(expectedProperties, reporting.getProperties());
        assertEquals("Line 1\nLine 2", reporting.getValue("label"));
    }

    @Test
    void everyStackedSharedEntryGetsCopiedInOrderAndUnrelatedEntriesStayUntouched() {
        SerializedElement first = entry("instance_identifier", LAN_IDENTIFIER);
        first.putProperty("x", "1");
        SerializedElement options = entry("instance_identifier", "pause_options_button");
        SerializedElement second = entry("instance_identifier", LAN_IDENTIFIER);
        second.putProperty("x", "2");
        List<SerializedElement> entries = new ArrayList<>(List.of(first, options, second));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION);

        assertEquals(5, entries.size());
        assertEquals(List.of(first, options, second), entries.subList(0, 3));
        assertEquals(REPORTING_IDENTIFIER, entries.get(3).getValue("instance_identifier"));
        assertEquals("1", entries.get(3).getValue("x"));
        assertEquals(REPORTING_IDENTIFIER, entries.get(4).getValue("instance_identifier"));
        assertEquals("2", entries.get(4).getValue("x"));
    }

    @Test
    void legacyActionIdentifierIsMigratedToo() {
        List<SerializedElement> entries = new ArrayList<>(List.of(entry("actionid", LAN_IDENTIFIER)));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION);

        assertEquals(2, entries.size());
        assertEquals(REPORTING_IDENTIFIER, WidgetIdentifierHandler.getTargetWidgetIdentifier(entries.get(1)));
    }

    @Test
    void currentRevisionKeepsLanOnlyCustomizationOffPlayerReporting() {
        List<SerializedElement> entries = new ArrayList<>(List.of(entry("instance_identifier", LAN_IDENTIFIER)));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.CURRENT_REVISION);

        assertEquals(1, entries.size());
    }

    @Test
    void existingPlayerReportingCustomizationIsNeverOverwritten() {
        SerializedElement lan = entry("instance_identifier", LAN_IDENTIFIER);
        SerializedElement reporting = entry("instance_identifier", "vanillabtn:" + REPORTING_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(lan, reporting));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION);

        assertEquals(List.of(lan, reporting), entries);
    }

    @Test
    void layoutWithoutSharedCustomizationStaysUnchanged() {
        List<SerializedElement> entries = new ArrayList<>(List.of(entry("instance_identifier", "pause_options_button"), new SerializedElement()));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION);

        assertEquals(2, entries.size());
    }

    private static SerializedElement entry(String identifierKey, String identifier) {
        SerializedElement serialized = new SerializedElement();
        serialized.setType("vanilla_button");
        serialized.putProperty("element_type", "vanilla_button");
        serialized.putProperty(identifierKey, identifier);
        return serialized;
    }

}
