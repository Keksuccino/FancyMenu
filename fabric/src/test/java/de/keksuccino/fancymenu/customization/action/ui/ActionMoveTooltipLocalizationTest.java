package de.keksuccino.fancymenu.customization.action.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActionMoveTooltipLocalizationTest {

    private static final String NO_ACTION_SELECTED_KEY = "fancymenu.actions.screens.finish.no_action_selected";

    @Test
    void englishLocalizationDefinesNoActionSelectedMessage() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        JsonElement message = localizations.get(NO_ACTION_SELECTED_KEY);

        assertNotNull(message, "The action move tooltip localization is missing");
        assertEquals("§xYou need to select an action first!", message.getAsString());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = ActionMoveTooltipLocalizationTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
