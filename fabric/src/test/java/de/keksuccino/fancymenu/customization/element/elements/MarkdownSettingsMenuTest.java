package de.keksuccino.fancymenu.customization.element.elements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettingsTest.TestElement;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The menu itself needs a running game, but its layout and the texts it asks for do not.
 */
class MarkdownSettingsMenuTest {

    private static final String MENU_LOCALIZATION_KEY_BASE = "fancymenu.elements.text.markdown.style.menu.";

    @Test
    void everyStyleOptionHasExactlyOneEntry() {
        TestElement element = new TestElement();
        List<MarkdownStyleOption<?>> entries = new ArrayList<>();
        collectOptions(MarkdownSettingsMenu.LAYOUT, element, entries);

        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            assertEquals(1, entries.stream().filter(entry -> entry == option).count(), option::getKey);
        }
        assertEquals(element.style().getOptions().size(), entries.size());
    }

    @Test
    void everySwitchOfTheSettingsHasExactlyOneEntry() {
        TestElement element = new TestElement();
        List<Property.BooleanProperty> toggles = new ArrayList<>();
        collectToggles(MarkdownSettingsMenu.LAYOUT, element, toggles);

        assertEquals(4, toggles.size());
        assertEquals(Set.of(element.settings.parseMarkdown, element.settings.extendedListMarkers, element.settings.numberedLists, element.settings.callouts), new HashSet<>(toggles));
    }

    @Test
    void entriesOfTheSameMenuHaveIdentifiersOfTheirOwn() {
        TestElement element = new TestElement();

        assertUniqueIdentifiers(MarkdownSettingsMenu.LAYOUT, element, "root");
    }

    @Test
    void onlyOptionsWithManyValuesNeedAListToPickFrom() {
        TestElement element = new TestElement();

        // A cycle through too many values is slow to use, and a list for two values is one click more than a cycle
        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            if (option instanceof EnumOption<?> enumOption) assertTrue(enumOption.getValues().length >= 2, option::getKey);
        }
        assertTrue(element.style().listLevel(1).markerShape.getValues().length > MarkdownSettingsMenu.MOST_VALUES_TO_CYCLE_THROUGH);
        assertTrue(element.style().headline(1).decoration.getValues().length <= MarkdownSettingsMenu.MOST_VALUES_TO_CYCLE_THROUGH);
    }

    @Test
    void everyTextOfTheMenuExistsInEnglish() throws IOException {
        JsonObject english = readEnglishLocalizations();
        TestElement element = new TestElement();
        List<String> missing = new ArrayList<>();

        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            String key = element.property(option).getContextMenuEntryLocalizationKeyBase();
            if (!english.has(key)) {
                missing.add(key);
                continue;
            }
            // Entries of switches and of options that show their value put it where the placeholder stands
            boolean showsValue = (option.getType() == MarkdownStyleOption.Type.BOOLEAN) || (option.getType() == MarkdownStyleOption.Type.ENUM);
            assertEquals(showsValue, english.get(key).getAsString().contains("%s"), key);
            if (option instanceof EnumOption<?> enumOption) {
                for (Enum<?> value : enumOption.getValues()) {
                    requireKey(english, MarkdownSettings.getValueLocalizationKey(value), missing);
                }
            }
        }
        for (Property.BooleanProperty toggle : List.of(element.settings.parseMarkdown, element.settings.extendedListMarkers, element.settings.numberedLists, element.settings.callouts)) {
            requireKey(english, toggle.getContextMenuEntryLocalizationKeyBase(), missing);
            if (english.has(toggle.getContextMenuEntryLocalizationKeyBase())) assertTrue(english.get(toggle.getContextMenuEntryLocalizationKeyBase()).getAsString().contains("%s"));
        }
        requireKey(english, element.settings.look.getContextMenuEntryLocalizationKeyBase(), missing);
        requireKey(english, element.settings.textScale.getContextMenuEntryLocalizationKeyBase(), missing);
        for (MarkdownLook look : MarkdownLook.values()) {
            requireKey(english, MarkdownSettings.getValueLocalizationKey(look), missing);
        }
        collectMissingMenuNames(MarkdownSettingsMenu.LAYOUT, english, missing);
        requireKey(english, "fancymenu.context_menu.entries.color.no_color", missing);

        assertTrue(missing.isEmpty(), () -> "missing in en_us.json: " + missing);
    }

    private static void requireKey(@NotNull JsonObject english, @NotNull String key, @NotNull List<String> missing) {
        if (!english.has(key) && !missing.contains(key)) missing.add(key);
    }

    private static void collectOptions(@NotNull List<MarkdownSettingsMenu.Item> items, @NotNull TestElement element, @NotNull List<MarkdownStyleOption<?>> options) {
        for (MarkdownSettingsMenu.Item item : items) {
            if (item instanceof MarkdownSettingsMenu.OptionItem optionItem) options.add(optionItem.option().apply(element.style()));
            if (item instanceof MarkdownSettingsMenu.MenuItem menuItem) collectOptions(menuItem.items(), element, options);
        }
    }

    private static void collectToggles(@NotNull List<MarkdownSettingsMenu.Item> items, @NotNull TestElement element, @NotNull List<Property.BooleanProperty> toggles) {
        for (MarkdownSettingsMenu.Item item : items) {
            if (item instanceof MarkdownSettingsMenu.ToggleItem toggleItem) toggles.add(toggleItem.property().apply(element.settings));
            if (item instanceof MarkdownSettingsMenu.MenuItem menuItem) collectToggles(menuItem.items(), element, toggles);
        }
    }

    private static void collectMissingMenuNames(@NotNull List<MarkdownSettingsMenu.Item> items, @NotNull JsonObject english, @NotNull List<String> missing) {
        for (MarkdownSettingsMenu.Item item : items) {
            if (item instanceof MarkdownSettingsMenu.MenuItem menuItem) {
                requireKey(english, MENU_LOCALIZATION_KEY_BASE + menuItem.name(), missing);
                assertFalse(menuItem.items().isEmpty(), menuItem::name);
                collectMissingMenuNames(menuItem.items(), english, missing);
            }
        }
    }

    /**
     * Menus of several selected elements get merged by the identifiers of their entries, so two entries of one menu must never share one.
     */
    private static void assertUniqueIdentifiers(@NotNull List<MarkdownSettingsMenu.Item> items, @NotNull TestElement element, @NotNull String menuName) {
        Set<String> identifiers = new HashSet<>();
        for (MarkdownSettingsMenu.Item item : items) {
            String identifier = null;
            if (item instanceof MarkdownSettingsMenu.OptionItem optionItem) identifier = element.property(optionItem.option().apply(element.style())).getKey();
            if (item instanceof MarkdownSettingsMenu.ToggleItem toggleItem) identifier = toggleItem.property().apply(element.settings).getKey();
            if (item instanceof MarkdownSettingsMenu.MenuItem menuItem) {
                identifier = "menu " + menuItem.name();
                assertUniqueIdentifiers(menuItem.items(), element, menuItem.name());
            }
            if (identifier != null) {
                String described = identifier;
                assertTrue(identifiers.add(identifier), () -> described + " stands twice in " + menuName);
            }
        }
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = MarkdownSettingsMenuTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
