package de.keksuccino.fancymenu.customization.action.actions.variables;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.variables.ListArrangement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What all list actions share: their identifiers, values, index checks and localizations.
 */
class ListActionsTest {

    private static final List<String> LANGUAGES = List.of("en_us", "de_de", "el_gr", "es_mx", "fr_fr", "ja_jp", "ko_kr", "pl_pl", "ru_ru", "uk_ua", "zh_cn");
    private static final List<String> LOCALIZATION_PREFIXES = List.of("fancymenu.actions.variables.list.", "fancymenu.actions.variables.add_to_list", "fancymenu.actions.variables.set_list", "fancymenu.actions.variables.remove_from_list", "fancymenu.actions.variables.clear_list", "fancymenu.actions.variables.arrange_list");

    private static List<AbstractListAction> actions() {
        return List.of(new AddToListAction(), new SetListEntryAction(), new RemoveFromListAction(), new ClearListAction(), new SetListAction(), new ArrangeListAction());
    }

    @Test
    void identifiersStayTheSame() {
        List<String> identifiers = new ArrayList<>();
        for (AbstractListAction action : actions()) identifiers.add(action.getIdentifier());
        assertEquals(List.of("add_to_list", "set_list_entry", "remove_from_list", "clear_list", "set_list", "arrange_list"), identifiers);
    }

    @Test
    void everyActionHasARawValueAndAUsablePreset() {
        for (AbstractListAction action : actions()) {
            String preset = action.getValuePreset();
            assertAll(action.getIdentifier(), () -> assertTrue(action.hasValue()), () -> assertTrue(action.hasRawValue()), () -> assertFalse(preset.isBlank()), () -> assertTrue(preset.startsWith("my_list")));
        }
    }

    @Test
    void everyActionTextIsLocalizedInEnglish() throws IOException {
        JsonObject english = readLocalizations("en_us");
        for (AbstractListAction action : actions()) {
            assertAll(action.getIdentifier(), () -> assertLocalized(english, action.getDisplayName()), () -> assertLocalized(english, action.getDescription()), () -> assertLocalized(english, Component.translatable("fancymenu.actions.variables." + action.getIdentifier() + ".edit_value")));
        }
        for (ListArrangement arrangement : ListArrangement.values()) {
            String key = "fancymenu.actions.variables.arrange_list.edit.arrangement." + arrangement.getSerializedName();
            assertAll(arrangement.name(), () -> assertLocalized(english, Component.translatable(key)), () -> assertLocalized(english, Component.translatable(key + ".desc")));
        }
        for (RemoveFromListAction.Mode mode : RemoveFromListAction.Mode.values()) {
            String serialized = serializedMode(mode);
            assertLocalized(english, Component.translatable("fancymenu.actions.variables.remove_from_list.edit.mode." + serialized));
        }
        for (SetListAction.Source source : SetListAction.Source.values()) {
            String serialized = serializedSource(source);
            assertLocalized(english, Component.translatable("fancymenu.actions.variables.set_list.edit.source." + serialized));
        }
    }

    @Test
    void everyLanguageHasAllListActionTexts() throws IOException {
        JsonObject english = readLocalizations("en_us");
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : english.entrySet()) {
            for (String prefix : LOCALIZATION_PREFIXES) {
                if (entry.getKey().startsWith(prefix)) keys.add(entry.getKey());
            }
        }
        assertFalse(keys.isEmpty());
        for (String language : LANGUAGES) {
            JsonObject localizations = readLocalizations(language);
            for (String key : keys) {
                JsonElement localization = localizations.get(key);
                assertNotNull(localization, "Missing " + language + " localization: " + key);
                assertFalse(localization.getAsString().isBlank(), "Blank " + language + " localization: " + key);
                // Placeholder arguments have to match, or the text breaks in that language
                assertEquals(countArguments(english.get(key).getAsString()), countArguments(localization.getAsString()), "Wrong argument count in " + language + ": " + key);
            }
        }
    }

    @Test
    void indexInputsMustBeWholeNumbersUnlessTheyHavePlaceholders() {
        assertAll(() -> assertTrue(AbstractListAction.isValidIndexInput("0", false)), () -> assertTrue(AbstractListAction.isValidIndexInput("-1", false)), () -> assertTrue(AbstractListAction.isValidIndexInput(" 2.0 ", false)),
                () -> assertFalse(AbstractListAction.isValidIndexInput("1.5", false)), () -> assertFalse(AbstractListAction.isValidIndexInput("abc", false)), () -> assertFalse(AbstractListAction.isValidIndexInput("99999999999", false)),
                () -> assertTrue(AbstractListAction.isValidIndexInput("{\"placeholder\":\"calc\",\"values\":{\"expression\":\"1+1\"}}", false)), () -> assertTrue(AbstractListAction.isValidIndexInput("$$position", false)),
                () -> assertFalse(AbstractListAction.isValidIndexInput("", false)), () -> assertFalse(AbstractListAction.isValidIndexInput("  ", false)), () -> assertTrue(AbstractListAction.isValidIndexInput("", true)), () -> assertTrue(AbstractListAction.isValidIndexInput(" ", true)));
    }

    @Test
    void valuesSplitIntoPartsWithTheRestInTheLastOne() {
        assertAll(() -> assertArrayEquals(new String[] {"a", "b", "c|||d"}, AbstractListAction.splitValue("a|||b|||c|||d", 3)), () -> assertArrayEquals(new String[] {"a", ""}, AbstractListAction.splitValue("a|||", 3)),
                () -> assertArrayEquals(new String[] {""}, AbstractListAction.splitValue("", 3)), () -> assertArrayEquals(new String[] {"a", "|b"}, AbstractListAction.splitValue("a||||b", 3)));
    }

    @Test
    void booleanPartsFallBackWhenTheyAreNoBooleans() {
        assertAll(() -> assertTrue(AbstractListAction.parseBoolean("true", false)), () -> assertTrue(AbstractListAction.parseBoolean(" TRUE ", false)), () -> assertFalse(AbstractListAction.parseBoolean("false", true)),
                () -> assertTrue(AbstractListAction.parseBoolean("yes", true)), () -> assertFalse(AbstractListAction.parseBoolean("", false)));
    }

    @NotNull
    private static String serializedMode(@NotNull RemoveFromListAction.Mode mode) {
        RemoveFromListAction.Config config = new RemoveFromListAction.Config();
        config.mode = mode;
        return AbstractListAction.splitValue(config.serialize(), 5)[1];
    }

    @NotNull
    private static String serializedSource(@NotNull SetListAction.Source source) {
        SetListAction.Config config = new SetListAction.Config();
        config.source = source;
        return AbstractListAction.splitValue(config.serialize(), 6)[1];
    }

    private static int countArguments(@NotNull String text) {
        int count = 0;
        int index = -1;
        while ((index = text.indexOf("%s", index + 1)) >= 0) count++;
        return count;
    }

    private static void assertLocalized(@NotNull JsonObject localizations, Component component) {
        assertNotNull(component);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, component.getContents());
        JsonElement localization = localizations.get(contents.getKey());
        assertNotNull(localization, "Missing English localization: " + contents.getKey());
        assertFalse(localization.getAsString().isBlank(), "Blank English localization: " + contents.getKey());
    }

    @NotNull
    private static JsonObject readLocalizations(@NotNull String language) throws IOException {
        try (InputStream stream = ListActionsTest.class.getResourceAsStream("/assets/fancymenu/lang/" + language + ".json")) {
            assertNotNull(stream, "Localization resource is missing from the test runtime classpath: " + language);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
