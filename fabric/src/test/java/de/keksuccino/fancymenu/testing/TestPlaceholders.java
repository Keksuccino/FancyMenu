package de.keksuccino.fancymenu.testing;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * <p>The placeholder registry is global and has no removal API, so all placeholders get registered once per test JVM.
 * Value placeholders are keyed, so every test should use its own keys to stay isolated from other tests.</p>
 */
public final class TestPlaceholders {

    private static final String ECHO_IDENTIFIER = "fancymenu_test_echo";
    private static final String VALUE_IDENTIFIER = "fancymenu_test_value";
    private static final String JOIN_IDENTIFIER = "fancymenu_test_join";
    private static final String NULL_IDENTIFIER = "fancymenu_test_null";
    private static final String FAILING_IDENTIFIER = "fancymenu_test_failing";
    private static final String CAPTURE_IDENTIFIER = "fancymenu_test_capture";
    private static final String MAIN_THREAD_VALUE_IDENTIFIER = "fancymenu_test_main_thread_value";
    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> EVALUATIONS = new ConcurrentHashMap<>();
    private static final Map<String, DeserializedPlaceholderString> CAPTURED = new ConcurrentHashMap<>();
    private static volatile Thread mainThread;

    static {
        register(ECHO_IDENTIFIER, List.of("text"), values -> values.get("text"));
        register(VALUE_IDENTIFIER, List.of("key"), values -> {
            String key = values.get("key");
            EVALUATIONS.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
            return VALUES.getOrDefault(key, "");
        });
        register(JOIN_IDENTIFIER, List.of("first", "second"), values -> values.get("first") + "|" + values.get("second"));
        register(NULL_IDENTIFIER, List.of("text"), values -> null);
        register(FAILING_IDENTIFIER, List.of("text"), values -> {
            throw new IllegalStateException("Test placeholder failure");
        });
        registerPlaceholder(new FunctionPlaceholder(CAPTURE_IDENTIFIER, List.of("key", "text"), null, false));
        registerPlaceholder(new FunctionPlaceholder(MAIN_THREAD_VALUE_IDENTIFIER, List.of("key"), values -> VALUES.getOrDefault(values.get("key"), ""), true));
    }

    private TestPlaceholders() {
    }

    /** A placeholder that gets replaced with the given text. */
    @NotNull
    public static String echo(@NotNull String text) {
        return serialize(ECHO_IDENTIFIER, "text", text);
    }

    /** A placeholder with two values that gets replaced with both values joined by "|". A missing value gets joined as "null". */
    @NotNull
    public static String join(@NotNull String first, @NotNull String second) {
        return "{\"placeholder\":\"" + JOIN_IDENTIFIER + "\",\"values\":{\"first\":\"" + first + "\",\"second\":\"" + second + "\"}}";
    }

    @NotNull
    public static String echoIdentifier() {
        return ECHO_IDENTIFIER;
    }

    @NotNull
    public static String joinIdentifier() {
        return JOIN_IDENTIFIER;
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

    /** A placeholder that returns null as replacement. */
    @NotNull
    public static String nullReplacement(@NotNull String text) {
        return serialize(NULL_IDENTIFIER, "text", text);
    }

    /** A placeholder that throws an exception when it gets replaced. */
    @NotNull
    public static String failing(@NotNull String text) {
        return serialize(FAILING_IDENTIFIER, "text", text);
    }

    /** A placeholder that remembers what it got called with under the given key (see {@link #captured(String)}) and gets replaced with "captured". */
    @NotNull
    public static String capture(@NotNull String key, @NotNull String text) {
        return "{\"placeholder\":\"" + CAPTURE_IDENTIFIER + "\",\"values\":{\"key\":\"" + key + "\",\"text\":\"" + text + "\"}}";
    }

    /** What the capture placeholder of the given key got called with the last time, or null if it never got called. */
    @Nullable
    public static DeserializedPlaceholderString captured(@NotNull String key) {
        return CAPTURED.get(key);
    }

    /**
     * Like {@link #value(String)}, but it can't run asynchronously, so it only gets replaced on the thread set with {@link #setMainThread(Thread)},
     * which stands in for the main thread of the game.
     */
    @NotNull
    public static String mainThreadValue(@NotNull String key) {
        return serialize(MAIN_THREAD_VALUE_IDENTIFIER, "key", key);
    }

    public static void setMainThread(@Nullable Thread thread) {
        mainThread = thread;
    }

    @NotNull
    private static String serialize(@NotNull String identifier, @NotNull String valueName, @NotNull String value) {
        return "{\"placeholder\":\"" + identifier + "\",\"values\":{\"" + valueName + "\":\"" + value + "\"}}";
    }

    private static void register(@NotNull String identifier, @NotNull List<String> valueNames, @NotNull Function<Map<String, String>, String> replacement) {
        registerPlaceholder(new FunctionPlaceholder(identifier, valueNames, replacement, false));
    }

    private static void registerPlaceholder(@NotNull FunctionPlaceholder placeholder) {
        if (PlaceholderRegistry.getPlaceholder(placeholder.getIdentifier()) == null) PlaceholderRegistry.register(placeholder);
    }

    private static final class FunctionPlaceholder extends Placeholder {

        private final List<String> valueNames;
        /** Null for the capture placeholder. */
        @Nullable
        private final Function<Map<String, String>, String> replacement;
        private final boolean mainThreadOnly;

        private FunctionPlaceholder(@NotNull String identifier, @NotNull List<String> valueNames, @Nullable Function<Map<String, String>, String> replacement, boolean mainThreadOnly) {
            super(identifier);
            this.valueNames = valueNames;
            this.replacement = replacement;
            this.mainThreadOnly = mainThreadOnly;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            if (this.replacement != null) return this.replacement.apply(dps.values);
            CAPTURED.put(dps.values.get("key"), new DeserializedPlaceholderString(dps.placeholderIdentifier, new HashMap<>(dps.values), dps.placeholderString));
            return "captured";
        }

        @Override
        public List<String> getValueNames() {
            return this.valueNames;
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
            return new DeserializedPlaceholderString(this.getIdentifier(), new HashMap<>(), serialize(this.getIdentifier(), this.valueNames.get(0), ""));
        }

        @Override
        public boolean canRunAsync() {
            return !this.mainThreadOnly;
        }

        // The default check needs a running Minecraft client to compare threads, so the main thread is the one set by the test.
        @Override
        public boolean checkAsync() {
            return this.canRunAsync() || (Thread.currentThread() == mainThread);
        }

    }

}
