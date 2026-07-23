package de.keksuccino.fancymenu.customization.requirement.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MouseClickedRequirementTest {

    @Test
    void englishTextDescribesHeldButtonState() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Mouse Button Is Pressed", localizations.get("fancymenu.requirements.mouse_click").getAsString());
        assertEquals("Returns true for as long as the specified mouse button is held down.\nUse 'left' or 'right' to select the button.", localizations.get("fancymenu.requirements.mouse_click.desc").getAsString());
    }

    @Test
    void serializedIdentifierRemainsBackwardCompatible() {
        assertEquals("mouse_click", new MouseClickedRequirement().getIdentifier());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = MouseClickedRequirementTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
