package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class WidgetIdentifierMigrationTest {

    private static final String LAN_IDENTIFIER = "pause_share_to_lan_button";
    private static final String REPORTING_IDENTIFIER = "pause_player_reporting_button";
    private static final String MODS_IDENTIFIER = "titlescreen_mods_button";
    private static final String FORGE_MODS_IDENTIFIER = "forge_titlescreen_mods_button";
    private static final String MOD_MENU_MODS_IDENTIFIER = "modmenu_titlescreen_mods_button";
    private static final String OPTIONS_IDENTIFIER = "mc_titlescreen_options_button";
    private static final Supplier<String> UNUSED_MOD_LOADER = () -> fail("Only layouts customizing the Mods button on more than one loader need the mod loader");

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

    @ParameterizedTest
    @CsvSource({
            "forge_titlescreen_mods_button, 0",
            "vanillabtn:forge_titlescreen_mods_button, 1",
            "button_compatibility_id:forge_titlescreen_mods_button, 0",
            "modmenu_titlescreen_mods_button, 1",
            "vanillabtn:modmenu_titlescreen_mods_button, 0",
            "button_compatibility_id:modmenu_titlescreen_mods_button, 1"
    })
    void olderLayoutMovesModsButtonCustomizationToTheSharedIdentifier(String storedIdentifier, int savedRevision) {
        SerializedElement mods = entry("instance_identifier", storedIdentifier);
        mods.putProperty("is_hidden", "true");
        mods.putProperty("label", "Line 1\nLine 2");
        Map<String, String> expectedProperties = new LinkedHashMap<>(mods.getProperties());
        expectedProperties.put("instance_identifier", MODS_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(mods));

        WidgetIdentifierMigration.migrate(entries, savedRevision, UNUSED_MOD_LOADER);

        assertEquals(List.of(mods), entries);
        assertEquals(expectedProperties, mods.getProperties());
    }

    @Test
    void legacyActionIdentifierOfModsButtonIsMigratedToo() {
        SerializedElement mods = entry("actionid", MOD_MENU_MODS_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(mods));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION, UNUSED_MOD_LOADER);

        assertEquals(List.of(mods), entries);
        assertEquals(MODS_IDENTIFIER, WidgetIdentifierHandler.getTargetWidgetIdentifier(mods));
    }

    @Test
    void fabricKeepsModMenuCustomizationOfLayoutCustomizedOnBothLoaders() {
        SerializedElement forge = entry("instance_identifier", FORGE_MODS_IDENTIFIER);
        SerializedElement options = entry("instance_identifier", OPTIONS_IDENTIFIER);
        SerializedElement modMenu = entry("instance_identifier", "vanillabtn:" + MOD_MENU_MODS_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(forge, options, modMenu));

        WidgetIdentifierMigration.migrate(entries, 1, () -> "fabric");

        assertEquals(List.of(options, modMenu), entries);
        assertEquals(MODS_IDENTIFIER, modMenu.getValue("instance_identifier"));
        assertEquals(OPTIONS_IDENTIFIER, options.getValue("instance_identifier"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"forge", "neoforge"})
    void forgeAndNeoForgeKeepTheirStackedCustomizationsOfLayoutCustomizedOnBothLoaders(String modLoader) {
        SerializedElement first = entry("instance_identifier", FORGE_MODS_IDENTIFIER);
        first.putProperty("x", "1");
        SerializedElement modMenu = entry("instance_identifier", MOD_MENU_MODS_IDENTIFIER);
        SerializedElement second = entry("instance_identifier", "button_compatibility_id:" + FORGE_MODS_IDENTIFIER);
        second.putProperty("x", "2");
        List<SerializedElement> entries = new ArrayList<>(List.of(first, modMenu, second));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.UNVERSIONED_REVISION, () -> modLoader);

        assertEquals(List.of(first, second), entries);
        assertEquals(MODS_IDENTIFIER, first.getValue("instance_identifier"));
        assertEquals("1", first.getValue("x"));
        assertEquals(MODS_IDENTIFIER, second.getValue("instance_identifier"));
        assertEquals("2", second.getValue("x"));
    }

    @Test
    void existingSharedModsButtonCustomizationWinsOverReplacedOnes() {
        SerializedElement forge = entry("instance_identifier", FORGE_MODS_IDENTIFIER);
        SerializedElement shared = entry("instance_identifier", "vanillabtn:" + MODS_IDENTIFIER);
        SerializedElement modMenu = entry("instance_identifier", MOD_MENU_MODS_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(forge, shared, modMenu));

        WidgetIdentifierMigration.migrate(entries, 1, UNUSED_MOD_LOADER);

        assertEquals(List.of(shared), entries);
        assertEquals("vanillabtn:" + MODS_IDENTIFIER, shared.getValue("instance_identifier"));
    }

    @Test
    void currentRevisionKeepsModsButtonCustomizationsUntouched() {
        SerializedElement forge = entry("instance_identifier", FORGE_MODS_IDENTIFIER);
        SerializedElement modMenu = entry("instance_identifier", MOD_MENU_MODS_IDENTIFIER);
        List<SerializedElement> entries = new ArrayList<>(List.of(forge, modMenu));

        WidgetIdentifierMigration.migrate(entries, WidgetIdentifierMigration.CURRENT_REVISION, UNUSED_MOD_LOADER);

        assertEquals(List.of(forge, modMenu), entries);
        assertEquals(FORGE_MODS_IDENTIFIER, forge.getValue("instance_identifier"));
        assertEquals(MOD_MENU_MODS_IDENTIFIER, modMenu.getValue("instance_identifier"));
    }

    @Test
    void replacedIdentifiersResolveToTheSharedModsButtonIdentifier() {
        assertEquals(MODS_IDENTIFIER, WidgetIdentifierMigration.getCurrentIdentifier(FORGE_MODS_IDENTIFIER));
        assertEquals(MODS_IDENTIFIER, WidgetIdentifierMigration.getCurrentIdentifier(MOD_MENU_MODS_IDENTIFIER));
        assertEquals(MODS_IDENTIFIER, WidgetIdentifierMigration.getCurrentIdentifier(MODS_IDENTIFIER));
        assertEquals(OPTIONS_IDENTIFIER, WidgetIdentifierMigration.getCurrentIdentifier(OPTIONS_IDENTIFIER));
    }

    private static SerializedElement entry(String identifierKey, String identifier) {
        SerializedElement serialized = new SerializedElement();
        serialized.setType("vanilla_button");
        serialized.putProperty("element_type", "vanilla_button");
        serialized.putProperty(identifierKey, identifier);
        return serialized;
    }

}
