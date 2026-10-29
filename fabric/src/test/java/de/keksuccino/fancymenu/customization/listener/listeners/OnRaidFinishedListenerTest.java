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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnRaidFinishedListenerTest {

    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");

    @Test
    void englishDescriptionDocumentsRuntimeVariableNames() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        String description = localizations.get("fancymenu.listeners.on_raid_finished.desc").getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        List<String> runtimeVariableNames = new OnRaidFinishedListener().getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(List.of("$$raid_result"), runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
    }

    @Test
    void raidResultReflectsHowTheRaidEnded() {
        OnRaidFinishedListener listener = new OnRaidFinishedListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onRaidFinished(true);
        assertEquals(OnRaidFinishedListener.RESULT_VICTORY, raidResultOf(instance));

        listener.onRaidFinished(false);
        assertEquals(OnRaidFinishedListener.RESULT_DEFEAT, raidResultOf(instance));
    }

    @Test
    void raidResultIsOnlyCachedWhileSomeoneListens() {
        OnRaidFinishedListener listener = new OnRaidFinishedListener();
        listener.onRaidFinished(true);

        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        assertEquals("ERROR", raidResultOf(instance));
    }

    @Test
    void raidListenersAreDescribedInEnglish() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        for (String key : List.of("on_raid_started", "on_enter_active_raid_area", "on_leave_active_raid_area", "on_raid_finished")) {
            assertNotNull(localizations.get("fancymenu.listeners." + key), key);
            assertNotNull(localizations.get("fancymenu.listeners." + key + ".desc"), key);
        }
    }

    private static String raidResultOf(ListenerInstance instance) {
        return instance.getActionScript().getValuePlaceholders().get("raid_result").get();
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OnRaidFinishedListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
