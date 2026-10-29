package de.keksuccino.fancymenu.customization.action.actions.variables;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteVariableActionTest {

    @Test
    void actionIdentifierRemainsBackwardCompatible() {
        assertEquals("delete_variable", new DeleteVariableAction().getIdentifier());
    }

    @Test
    void actionRequiresVariableNameValueWithUsablePreset() {
        DeleteVariableAction action = new DeleteVariableAction();
        String preset = action.getValuePreset();

        assertAll(() -> assertTrue(action.hasValue()), () -> assertFalse(action.hasRawValue()), () -> assertNotNull(preset), () -> assertFalse(preset.isBlank()), () -> assertEquals(preset.strip(), preset), () -> assertFalse(preset.contains(":")));
    }

    @Test
    void everyActionTextIsLocalizedInEnglish() throws IOException {
        DeleteVariableAction action = new DeleteVariableAction();
        JsonObject localizations = readEnglishLocalizations();

        assertAll(() -> assertLocalized(localizations, action.getDisplayName()), () -> assertLocalized(localizations, action.getDescription()), () -> assertLocalized(localizations, action.getValueDisplayName()));
    }

    private static void assertLocalized(@NotNull JsonObject localizations, Component component) {
        assertNotNull(component);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, component.getContents());
        JsonElement localization = localizations.get(contents.getKey());
        assertNotNull(localization, "Missing English localization: " + contents.getKey());
        assertFalse(localization.getAsString().isBlank(), "Blank English localization: " + contents.getKey());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = DeleteVariableActionTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
