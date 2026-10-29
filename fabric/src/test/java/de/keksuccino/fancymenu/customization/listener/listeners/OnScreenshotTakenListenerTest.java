package de.keksuccino.fancymenu.customization.listener.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnScreenshotTakenListenerTest {

    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");
    private static final File SCREENSHOT_FILE = new File("instance/screenshots/2026-10-29_14.30.00.png");
    private static final String SCREENSHOT_PATH = "/screenshots/2026-10-29_14.30.00.png";

    @BeforeEach
    @AfterEach
    void clearMainThreadQueue() {
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    @Test
    void englishDescriptionDocumentsRuntimeVariableNames() throws IOException {
        JsonObject localizations = readEnglishLocalizations();
        assertTrue(localizations.has("fancymenu.listeners.on_screenshot_taken"));
        String description = localizations.get("fancymenu.listeners.on_screenshot_taken.desc").getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        List<String> runtimeVariableNames = new OnScreenshotTakenListener().getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(List.of("$$screenshot_path"), runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
    }

    @Test
    void screenshotEventMapsPathToRuntimeVariable() {
        OnScreenshotTakenListener listener = new OnScreenshotTakenListener();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onScreenshotTaken(SCREENSHOT_PATH);

        assertEquals(SCREENSHOT_PATH, screenshotPathOf(instance));
    }

    @Test
    void savedScreenshotIsDeferredToMainThread() {
        AtomicInteger formatCalls = new AtomicInteger();
        OnScreenshotTakenListener listener = new OnScreenshotTakenListener(file -> { formatCalls.incrementAndGet(); return "/screenshots/" + file.getName(); });
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        listener.onScreenshotSaved(SCREENSHOT_FILE);

        assertEquals("", screenshotPathOf(instance));
        assertEquals(1, formatCalls.get());
        assertEquals(1, runQueuedMainThreadTasks());
        assertEquals(SCREENSHOT_PATH, screenshotPathOf(instance));
    }

    @Test
    void savedScreenshotIsIgnoredWithoutListeningInstances() {
        AtomicInteger formatCalls = new AtomicInteger();
        OnScreenshotTakenListener listener = new OnScreenshotTakenListener(file -> { formatCalls.incrementAndGet(); return SCREENSHOT_PATH; });

        listener.onScreenshotSaved(SCREENSHOT_FILE);

        assertEquals(0, formatCalls.get());
        assertEquals(0, runQueuedMainThreadTasks());
    }

    @Test
    void savedScreenshotIsDroppedWhenInstancesChangeBeforeDispatch() {
        OnScreenshotTakenListener listener = new OnScreenshotTakenListener(file -> SCREENSHOT_PATH);
        ListenerInstance oldInstance = listener.createFreshInstance();
        oldInstance.registerSelfToParent();

        listener.onScreenshotSaved(SCREENSHOT_FILE);
        listener.unregisterInstance(oldInstance);
        ListenerInstance newInstance = listener.createFreshInstance();
        newInstance.registerSelfToParent();

        assertEquals(1, runQueuedMainThreadTasks());
        assertEquals("", screenshotPathOf(newInstance));
    }

    @Test
    void failingPathFormatterDoesNotEscapeIntoScreenshotSaving() {
        OnScreenshotTakenListener listener = new OnScreenshotTakenListener(file -> { throw new IllegalStateException("Path formatting failed"); });
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        assertDoesNotThrow(() -> listener.onScreenshotSaved(SCREENSHOT_FILE));
        assertEquals(0, runQueuedMainThreadTasks());
        assertEquals("", screenshotPathOf(instance));
    }

    private static String screenshotPathOf(ListenerInstance instance) {
        return instance.getActionScript().getValuePlaceholders().get("screenshot_path").get();
    }

    private static int runQueuedMainThreadTasks() {
        List<Runnable> tasks = MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        tasks.forEach(Runnable::run);
        return tasks.size();
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OnScreenshotTakenListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
