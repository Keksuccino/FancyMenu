package de.keksuccino.fancymenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RinkuDependencyMetadataTest {

    @Test
    void fabricMetadataMakesRinkuOptionalAndRejectsOldVersions() throws IOException {
        JsonObject metadata = readMetadata();
        JsonObject dependencies = metadata.getAsJsonObject("depends");
        JsonObject suggestions = metadata.getAsJsonObject("suggests");
        JsonObject incompatibilities = metadata.getAsJsonObject("breaks");

        assertNotNull(dependencies);
        assertNotNull(suggestions);
        assertNotNull(incompatibilities);
        assertFalse(dependencies.has("rinku"));
        assertEquals(">=3.0.4", suggestions.get("rinku").getAsString());
        assertEquals("<3.0.4", incompatibilities.get("rinku").getAsString());
    }

    private static JsonObject readMetadata() throws IOException {
        InputStream input = RinkuDependencyMetadataTest.class.getClassLoader().getResourceAsStream("fabric.mod.json");
        assertNotNull(input, "Missing processed fabric.mod.json");

        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}

