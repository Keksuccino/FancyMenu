package de.keksuccino.fancymenu.customization.listener.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnPositionChangedListenerTest {

    private static final List<String> EXPECTED_VARIABLE_NAMES = List.of("$$old_pos_x", "$$old_pos_y", "$$old_pos_z", "$$new_pos_x", "$$new_pos_y", "$$new_pos_z");
    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");

    @Test
    void englishDescriptionDocumentsRuntimeVariableNames() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        String description = localizations.get("fancymenu.listeners.on_position_changed.desc").getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        OnPositionChangedListener listener = new OnPositionChangedListener();
        List<String> runtimeVariableNames = listener.getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(EXPECTED_VARIABLE_NAMES, runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
    }

    @Test
    void positionEventMapsOldAndNewCoordinatesToRuntimeVariables() {
        OnPositionChangedListener listener = new OnPositionChangedListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onPositionChanged(new BlockPos(-12, 64, 320), new BlockPos(-11, 65, 319));

        Map<String, Supplier<String>> variables = instance.getActionScript().getValuePlaceholders();
        assertEquals("-12", variables.get("old_pos_x").get());
        assertEquals("64", variables.get("old_pos_y").get());
        assertEquals("320", variables.get("old_pos_z").get());
        assertEquals("-11", variables.get("new_pos_x").get());
        assertEquals("65", variables.get("new_pos_y").get());
        assertEquals("319", variables.get("new_pos_z").get());
        assertEquals("-12|64|320|-11|65|319", ValuePlaceholderHolder.applyValuePlaceholders(String.join("|", EXPECTED_VARIABLE_NAMES), variables));
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OnPositionChangedListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
