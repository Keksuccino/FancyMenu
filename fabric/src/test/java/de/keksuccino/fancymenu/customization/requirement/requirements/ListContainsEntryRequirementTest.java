package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.requirements.ListContainsEntryRequirement.ListEntryCheck;
import de.keksuccino.fancymenu.testing.TestLocalizations;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The requirement reads the entries through a lookup that answers like {@code VariableHandler#getListEntries(String)}: the entries of list
 * variables, and null for missing variables and text variables. The lists here stand in for the variables, so the tests never touch the
 * variables of the game directory.
 */
class ListContainsEntryRequirementTest {

    private static final Map<String, List<String>> LISTS = Map.of("fruits", List.of("apple", "Banana", "a:b", "true:x"), "empty", List.of(), "with_empty", List.of("a", ""));
    /** Missing variables and text variables both have no entries, so the lookup answers both with null. */
    private static final ListContainsEntryRequirement REQUIREMENT = new ListContainsEntryRequirement(LISTS::get);

    private static boolean isMet(String listName, String entry, boolean ignoreCase) {
        return REQUIREMENT.isRequirementMet(new ListEntryCheck(listName, entry, ignoreCase).serialize());
    }

    @Test
    void requirementKeepsItsIdentifierAndLocalizationKeys() {
        TranslatableContents displayName = assertInstanceOf(TranslatableContents.class, REQUIREMENT.getDisplayName().getContents());
        TranslatableContents description = assertInstanceOf(TranslatableContents.class, REQUIREMENT.getDescription().getContents());
        assertAll(() -> assertEquals("list_contains_entry", REQUIREMENT.getIdentifier()), () -> assertTrue(REQUIREMENT.hasValue()), () -> assertEquals("fancymenu.requirements.list_contains_entry", displayName.getKey()),
                () -> assertEquals("fancymenu.requirements.list_contains_entry.desc", description.getKey()));
    }

    @Test
    void isMetWhenTheListHasTheWholeEntry() {
        assertAll(() -> assertTrue(isMet("fruits", "apple", false)), () -> assertTrue(isMet("fruits", "Banana", false)), () -> assertFalse(isMet("fruits", "banana", false)), () -> assertFalse(isMet("fruits", "app", false)),
                () -> assertFalse(isMet("fruits", "apple ", false)), () -> assertFalse(isMet("fruits", "grape", false)));
    }

    @Test
    void ignoringCaseMatchesEntriesWithOtherCase() {
        assertAll(() -> assertTrue(isMet("fruits", "banana", true)), () -> assertTrue(isMet("fruits", "APPLE", true)), () -> assertFalse(isMet("fruits", "appl", true)));
    }

    @Test
    void entriesCanContainSeparatorsAndFlagLikeText() {
        assertAll(() -> assertTrue(isMet("fruits", "a:b", false)), () -> assertTrue(isMet("fruits", "true:x", false)), () -> assertTrue(REQUIREMENT.isRequirementMet("fruits:false:a:b")), () -> assertTrue(REQUIREMENT.isRequirementMet("fruits:a:b")));
    }

    @Test
    void emptyEntriesAreEntriesToo() {
        assertAll(() -> assertTrue(isMet("with_empty", "", false)), () -> assertTrue(REQUIREMENT.isRequirementMet("with_empty")), () -> assertFalse(isMet("empty", "", false)), () -> assertFalse(isMet("fruits", "", false)));
    }

    @Test
    void isNeverMetForMissingAndTextVariablesOrWithoutAName() {
        assertAll(() -> assertFalse(isMet("text_or_missing", "apple", false)), () -> assertFalse(isMet("text_or_missing", "apple", true)), () -> assertFalse(REQUIREMENT.isRequirementMet(null)),
                () -> assertFalse(REQUIREMENT.isRequirementMet("")), () -> assertFalse(REQUIREMENT.isRequirementMet(":false:apple")), () -> assertFalse(REQUIREMENT.isRequirementMet("  :apple")));
    }

    @Test
    void namesGetTrimmedLikeEverywhereElse() {
        assertAll(() -> assertTrue(REQUIREMENT.isRequirementMet(" fruits :false:apple")), () -> assertEquals("fruits", ListEntryCheck.parse("\tfruits :apple").listName()));
    }

    @Test
    void serializedChecksReadBackTheSame() {
        List<ListEntryCheck> checks = List.of(new ListEntryCheck("fruits", "apple", false), new ListEntryCheck("fruits", "apple", true), new ListEntryCheck("fruits", "", false), new ListEntryCheck("fruits", "", true),
                new ListEntryCheck("fruits", "a:b:c", true), new ListEntryCheck("fruits", "true:x", false), new ListEntryCheck("fruits", "false:", true), new ListEntryCheck("fruits", ":", false), new ListEntryCheck("fruits", " spaced ", false),
                new ListEntryCheck("fruits", "line\nbreak", false), new ListEntryCheck("fruits", "Grüße 😀 {}", true), new ListEntryCheck("fruits", "{\"placeholder\":\"getvariable\",\"values\":{\"name\":\"x\"}}", false),
                new ListEntryCheck("{\"placeholder\":\"getvariable\",\"values\":{\"name\":\"which_list\"}}", "a:b", true), new ListEntryCheck("", "apple", false), new ListEntryCheck("", "", false));
        for (ListEntryCheck check : checks) {
            String serialized = check.serialize();
            assertEquals(check, ListEntryCheck.parse(serialized), serialized);
            assertEquals(serialized, ListEntryCheck.parse(serialized).serialize(), serialized);
        }
    }

    @Test
    void missingPartsGetDefaults() {
        assertAll(() -> assertEquals(new ListEntryCheck("fruits", "", false), ListEntryCheck.parse("fruits")), () -> assertEquals(new ListEntryCheck("fruits", "apple", false), ListEntryCheck.parse("fruits:apple")),
                () -> assertEquals(new ListEntryCheck("fruits", "apple:pie", false), ListEntryCheck.parse("fruits:apple:pie")), () -> assertEquals(new ListEntryCheck("fruits", "true", false), ListEntryCheck.parse("fruits:true")),
                () -> assertEquals(new ListEntryCheck("fruits", "", true), ListEntryCheck.parse("fruits:true:")), () -> assertEquals(new ListEntryCheck("", "", false), ListEntryCheck.parse("")));
    }

    @Test
    void presetIsAValidValue() {
        ListEntryCheck preset = ListEntryCheck.parse(REQUIREMENT.getValuePreset());
        assertAll(() -> assertFalse(preset.listName().isEmpty()), () -> assertFalse(preset.entry().isEmpty()), () -> assertEquals(REQUIREMENT.getValuePreset(), preset.serialize()));
    }

    @Test
    void everyTextIsLocalizedInEveryLanguage() throws Exception {
        String prefix = "fancymenu.requirements.list_contains_entry";
        TestLocalizations.assertLocalizedEverywhere(List.of(prefix, prefix + ".desc", prefix + ".value.desc", prefix + ".list_name", prefix + ".entry", prefix + ".entry.desc", prefix + ".ignore_case", prefix + ".ignore_case.desc", prefix + ".text_variable_hint"));
        // The cycle button puts its value into the label
        TestLocalizations.readAllLanguages().forEach((file, translations) -> assertTrue(translations.get(prefix + ".ignore_case").getAsString().contains("%s"), file));
    }

}
