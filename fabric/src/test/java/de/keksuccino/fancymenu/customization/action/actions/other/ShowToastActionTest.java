package de.keksuccino.fancymenu.customization.action.actions.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShowToastActionTest {

    @Test
    void englishWidthHelpDocumentsRuntimeClampContract() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("The toast width in pixels. The accepted range is 120–320 pixels, inclusive; values outside this range are clamped to the nearest limit. The default width is 160 pixels.", localizations.get("fancymenu.actions.show_toast.edit.width.desc").getAsString());
    }

    @Test
    void parsedWidthsUseDocumentedDefaultAndInclusiveClampBounds() {
        ShowToastAction.ToastConfig defaultConfig = ShowToastAction.ToastConfig.parse(null);
        assertNotNull(defaultConfig);
        assertEquals(160, defaultConfig.width);
        assertEquals(120, parseWidth(Integer.MIN_VALUE));
        assertEquals(120, parseWidth(120));
        assertEquals(160, parseWidth(160));
        assertEquals(320, parseWidth(320));
        assertEquals(320, parseWidth(Integer.MAX_VALUE));
    }

    @Test
    void actionIdentifierRemainsBackwardCompatible() {
        assertEquals("show_toast", new ShowToastAction().getIdentifier());
    }

    private static int parseWidth(int width) {
        ShowToastAction.ToastConfig config = ShowToastAction.ToastConfig.parse("{\"width\":" + width + "}");
        assertNotNull(config);
        return config.width;
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = ShowToastActionTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
