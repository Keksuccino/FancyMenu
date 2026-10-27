package de.keksuccino.fancymenu.testing;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Registered test placeholders for code that parses placeholders through the real {@code PlaceholderParser}.
 *
 * <p>The placeholder registry is global and has no removal API, so both placeholders get registered once per test JVM.
 * Value placeholders are keyed, so every test should use its own keys to stay isolated from other tests.</p>
 */
public final class TestPlaceholders {

    private static final String ECHO_IDENTIFIER = "fancymenu_test_echo";
    private static final String VALUE_IDENTIFIER = "fancymenu_test_value";
    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> EVALUATIONS = new ConcurrentHashMap<>();

    static {
        register(ECHO_IDENTIFIER, "text", values -> values.get("text"));
        register(VALUE_IDENTIFIER, "key", values -> {
            String key = values.get("key");
            EVALUATIONS.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
            return VALUES.getOrDefault(key, "");
        });
    }

    private TestPlaceholders() {
    }

    /** A placeholder that gets replaced with the given text. */
    @NotNull
    public static String echo(@NotNull String text) {
        return serialize(ECHO_IDENTIFIER, "text", text);
    }

    /** A placeholder that gets replaced with the current value of the given key, see {@link #setValue(String, String)}. */
    @NotNull
    public static String value(@NotNull String key) {
        return serialize(VALUE_IDENTIFIER, "key", key);
    }

    public static void setValue(@NotNull String key, @NotNull String value) {
        VALUES.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
    }

    /** How often the value placeholder of the given key got evaluated. */
    public static int evaluations(@NotNull String key) {
        AtomicInteger evaluations = EVALUATIONS.get(key);
        return (evaluations != null) ? evaluations.get() : 0;
    }

    @NotNull
    private static String serialize(@NotNull String identifier, @NotNull String valueName, @NotNull String value) {
        return "{\"placeholder\":\"" + identifier + "\",\"values\":{\"" + valueName + "\":\"" + value + "\"}}";
    }

    private static void register(@NotNull String identifier, @NotNull String valueName, @NotNull Function<Map<String, String>, String> replacement) {
        if (PlaceholderRegistry.getPlaceholder(identifier) == null) PlaceholderRegistry.register(new FunctionPlaceholder(identifier, valueName, replacement));
    }

    private static final class FunctionPlaceholder extends Placeholder {

        private final String valueName;
        private final Function<Map<String, String>, String> replacement;

        private FunctionPlaceholder(@NotNull String identifier, @NotNull String valueName, @NotNull Function<Map<String, String>, String> replacement) {
            super(identifier);
            this.valueName = valueName;
            this.replacement = replacement;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            return this.replacement.apply(dps.values);
        }

        @Override
        public List<String> getValueNames() {
            return List.of(this.valueName);
        }

        @Override
        public @NotNull String getDisplayName() {
            return this.getIdentifier();
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
            return new DeserializedPlaceholderString(this.getIdentifier(), new HashMap<>(), serialize(this.getIdentifier(), this.valueName, ""));
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
