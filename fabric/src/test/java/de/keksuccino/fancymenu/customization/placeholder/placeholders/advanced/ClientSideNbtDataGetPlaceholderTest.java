package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity selectors that sort randomly pick a new entity on every call, so only they make the placeholder get evaluated per occurrence.
 */
class ClientSideNbtDataGetPlaceholderTest {

    private static final ClientSideNbtDataGetPlaceholder PLACEHOLDER = new ClientSideNbtDataGetPlaceholder();

    @ParameterizedTest
    @ValueSource(strings = {"@r", " @r ", "@r[limit=2]", "@e[sort=random]", "@a[type=minecraft:player,sort=RANDOM]", "@p[sort=random]", "@e[sort=nearest,sort=random]"})
    void randomlySortingSelectorsAreEvaluatedPerOccurrence(String selector) {
        assertTrue(PLACEHOLDER.isEvaluatedPerOccurrence(dps("entity", selector)));
        assertTrue(PLACEHOLDER.isEvaluatedPerOccurrence(dps("ENTITY", selector)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"@s", "@p", "@a", "@e", "@e[sort=nearest]", "@r[sort=furthest]", "@e[sort=random,sort=arbitrary]", "@e[sort=unknown_order]", "Steve", "", "@x"})
    void otherSelectorsAreNotEvaluatedPerOccurrence(String selector) {
        assertFalse(PLACEHOLDER.isEvaluatedPerOccurrence(dps("entity", selector)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"block", ""})
    void otherSourcesAreNotEvaluatedPerOccurrence(String sourceType) {
        assertFalse(PLACEHOLDER.isEvaluatedPerOccurrence(dps(sourceType, "@r")));
        assertFalse(PLACEHOLDER.isEvaluatedPerOccurrence(dps(null, "@r")));
        assertFalse(PLACEHOLDER.isEvaluatedPerOccurrence(dps("entity", null)));
    }

    @NotNull
    private static DeserializedPlaceholderString dps(@Nullable String sourceType, @Nullable String selector) {
        HashMap<String, String> values = new HashMap<>();
        if (sourceType != null) values.put("source_type", sourceType);
        if (selector != null) values.put("entity_selector", selector);
        values.put("nbt_path", "Health");
        return new DeserializedPlaceholderString("nbt_data_get", values, "");
    }

}
