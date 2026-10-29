package de.keksuccino.fancymenu.customization.listener.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbstractServerIpListenerTest {

    private static final Pattern DOCUMENTED_VARIABLE_PATTERN = Pattern.compile("§z(\\$+[a-z0-9_]+) §r=");

    static Stream<ServerIpListenerCase> serverIpListeners() {
        return Stream.of(
                new ServerIpListenerCase("start_joining_server", "fancymenu.listeners.on_start_joining_server.desc", OnStartJoiningServerListener::new, (listener, serverIp) -> ((OnStartJoiningServerListener) listener).onStartJoiningServer(serverIp)),
                new ServerIpListenerCase("server_joined", "fancymenu.listeners.on_server_joined.desc", OnServerJoinedListener::new, (listener, serverIp) -> ((OnServerJoinedListener) listener).onServerJoined(serverIp)),
                new ServerIpListenerCase("server_left", "fancymenu.listeners.on_server_left.desc", OnServerLeftListener::new, (listener, serverIp) -> ((OnServerLeftListener) listener).onServerLeft(serverIp))
        );
    }

    @ParameterizedTest
    @MethodSource("serverIpListeners")
    void eventMapsServerIpToRuntimeVariable(ServerIpListenerCase testCase) {
        AbstractServerIpListener listener = testCase.factory().get();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();

        testCase.fire().accept(listener, "play.example.net:25570");

        assertEquals(testCase.identifier(), listener.getIdentifier());
        assertEquals("play.example.net:25570", ValuePlaceholderHolder.applyValuePlaceholders("$$server_ip", instance.getActionScript().getValuePlaceholders()));
    }

    @ParameterizedTest
    @MethodSource("serverIpListeners")
    void missingOrBlankServerIpFallsBackToErrorValue(ServerIpListenerCase testCase) {
        AbstractServerIpListener listener = testCase.factory().get();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();
        Supplier<String> serverIpVariable = instance.getActionScript().getValuePlaceholders().get("server_ip");

        assertEquals("ERROR", serverIpVariable.get());
        testCase.fire().accept(listener, null);
        assertEquals("ERROR", serverIpVariable.get());
        testCase.fire().accept(listener, " ");
        assertEquals("ERROR", serverIpVariable.get());
    }

    @ParameterizedTest
    @MethodSource("serverIpListeners")
    void eventWithoutListeningInstancesKeepsPreviousServerIp(ServerIpListenerCase testCase) {
        AbstractServerIpListener listener = testCase.factory().get();
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();
        testCase.fire().accept(listener, "first.example.net");

        listener.unregisterInstance(instance);
        testCase.fire().accept(listener, "second.example.net");

        assertEquals("first.example.net", instance.getActionScript().getValuePlaceholders().get("server_ip").get());
    }

    @ParameterizedTest
    @MethodSource("serverIpListeners")
    void eventTriggeredByOwnActionsDoesNotRecurse(ServerIpListenerCase testCase) {
        AbstractServerIpListener listener = testCase.factory().get();
        ListenerInstance instance = listener.createFreshInstance();
        Supplier<String> serverIpVariable = instance.getActionScript().getValuePlaceholders().get("server_ip");
        List<String> seenServerIps = new ArrayList<>();
        instance.getActionScript().addExecutable(new RunnableExecutable(() -> {
            seenServerIps.add(serverIpVariable.get());
            testCase.fire().accept(listener, "nested.example.net");
            seenServerIps.add(serverIpVariable.get());
        }));
        instance.registerSelfToParent();

        testCase.fire().accept(listener, "outer.example.net");
        testCase.fire().accept(listener, "next.example.net");

        assertEquals(List.of("outer.example.net", "outer.example.net", "next.example.net", "next.example.net"), seenServerIps);
    }

    @ParameterizedTest
    @MethodSource("serverIpListeners")
    void englishDescriptionDocumentsRuntimeVariableNames(ServerIpListenerCase testCase) throws IOException {
        String description = readEnglishLocalizations().get(testCase.descriptionKey()).getAsString();
        List<String> documentedVariableNames = DOCUMENTED_VARIABLE_PATTERN.matcher(description).results().map(result -> result.group(1)).toList();
        List<String> runtimeVariableNames = testCase.factory().get().getCustomVariables().stream().map(variable -> ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + variable.name()).toList();

        assertEquals(List.of("$$server_ip"), runtimeVariableNames);
        assertEquals(runtimeVariableNames, documentedVariableNames);
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = AbstractServerIpListenerTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private record RunnableExecutable(Runnable task) implements Executable {

        @Override
        public void execute() {
            this.task.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return "runnable_test_executable";
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            throw new UnsupportedOperationException();
        }

    }

    record ServerIpListenerCase(String identifier, String descriptionKey, Supplier<AbstractServerIpListener> factory, BiConsumer<AbstractServerIpListener, String> fire) {

        @Override
        public String toString() {
            return this.identifier;
        }

    }

}
