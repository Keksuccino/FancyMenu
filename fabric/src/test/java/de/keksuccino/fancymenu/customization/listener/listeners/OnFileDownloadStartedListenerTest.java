package de.keksuccino.fancymenu.customization.listener.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnFileDownloadStartedListenerTest {

    private static final List<String> EXPECTED_VARIABLE_NAMES = List.of("$$download_url", "$$target_file_path");
    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");

    @Test
    void englishDescriptionDocumentsRuntimeVariableNames() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        String description = localizations.get("fancymenu.listeners.on_file_download_started.desc").getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        OnFileDownloadStartedListener listener = new OnFileDownloadStartedListener();
        List<String> runtimeVariableNames = listener.getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(EXPECTED_VARIABLE_NAMES, runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
        assertNotNull(localizations.get("fancymenu.listeners.on_file_download_started"));
    }

    @Test
    void downloadStartedEventMapsUrlAndTargetPathToRuntimeVariables() {
        OnFileDownloadStartedListener listener = new OnFileDownloadStartedListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onFileDownloadStarted("https://example.com/am_update.arsa", "/game/config/downloads/am_update.arsa");

        Map<String, Supplier<String>> variables = instance.getActionScript().getValuePlaceholders();
        assertEquals("https://example.com/am_update.arsa", variables.get("download_url").get());
        assertEquals("/game/config/downloads/am_update.arsa", variables.get("target_file_path").get());
        assertEquals("https://example.com/am_update.arsa|/game/config/downloads/am_update.arsa", ValuePlaceholderHolder.applyValuePlaceholders(String.join("|", EXPECTED_VARIABLE_NAMES), variables));
    }

    @Test
    void eventWithoutListeningInstancesIsNotCached() {
        OnFileDownloadStartedListener listener = new OnFileDownloadStartedListener();

        listener.onFileDownloadStarted("https://example.com/ignored.txt", "/game/ignored.txt");
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        Map<String, Supplier<String>> variables = instance.getActionScript().getValuePlaceholders();
        assertNotEquals("https://example.com/ignored.txt", variables.get("download_url").get());
        assertNotEquals("/game/ignored.txt", variables.get("target_file_path").get());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OnFileDownloadStartedListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
