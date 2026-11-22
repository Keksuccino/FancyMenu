package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.placeholder.LogCooldownTracker;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.ListArrangement;
import de.keksuccino.fancymenu.customization.variables.ListEditResult;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Base of the actions that edit list variables. Their values have parts separated by {@link #VALUE_SEPARATOR}, with the part that can
 * hold any text last, and they get edited in their own window (see {@link ListActionValueWindowBody}).
 *
 * <p>An edit that can't work, like one of a text variable with a value or of an index the list doesn't have, logs a warning. Actions run in
 * loops and tickers, so the same warning of the same action and list only gets logged once per {@link #WARNING_COOLDOWN_MILLIS}.</p>
 */
public abstract class AbstractListAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    static final String VALUE_SEPARATOR = "|||";
    private static final Pattern VALUE_SEPARATOR_PATTERN = Pattern.compile(VALUE_SEPARATOR, Pattern.LITERAL);
    static final String LOCALIZATION_KEY_PREFIX = "fancymenu.actions.variables.";
    private static final long WARNING_COOLDOWN_MILLIS = 10000L;
    private static final int WARNING_COOLDOWN_MAX_ENTRIES = 256;
    // The key of a warning holds the list name, which can be long when it comes from a placeholder. A key heavier than this would never get its cooldown.
    private static final long WARNING_COOLDOWN_MAX_WEIGHT = 1_048_576L;
    private static final LogCooldownTracker WARNING_COOLDOWN = new LogCooldownTracker(WARNING_COOLDOWN_MILLIS, WARNING_COOLDOWN_MAX_ENTRIES, WARNING_COOLDOWN_MAX_WEIGHT);
    /** How many characters of an entry or text the summaries of values show. */
    private static final int SUMMARY_TEXT_LIMIT = 32;
    /** Edits the list variables of the game. */
    static final ListVariableAccess VARIABLE_HANDLER = new ListVariableAccess() {
        @Override
        public @NotNull ListEditResult setList(@NotNull String name, @NotNull List<String> entries) {
            return VariableHandler.setList(name, entries);
        }
        @Override
        public @NotNull ListEditResult addListEntry(@NotNull String name, @NotNull String entry, @Nullable Integer index, boolean skipIfContained, boolean ignoreCase) {
            return VariableHandler.addListEntry(name, entry, index, skipIfContained, ignoreCase);
        }
        @Override
        public @NotNull ListEditResult setListEntry(@NotNull String name, int index, @NotNull String entry) {
            return VariableHandler.setListEntry(name, index, entry);
        }
        @Override
        public @NotNull ListEditResult removeListEntryAt(@NotNull String name, int index) {
            return VariableHandler.removeListEntryAt(name, index);
        }
        @Override
        public @NotNull ListEditResult removeListEntries(@NotNull String name, @NotNull String entry, boolean allMatches, boolean ignoreCase) {
            return VariableHandler.removeListEntries(name, entry, allMatches, ignoreCase);
        }
        @Override
        public @NotNull ListEditResult clearList(@NotNull String name) {
            return VariableHandler.clearList(name);
        }
        @Override
        public @NotNull ListEditResult arrangeList(@NotNull String name, @NotNull ListArrangement arrangement) {
            return VariableHandler.arrangeList(name, arrangement);
        }
    };

    @NotNull
    protected final ListVariableAccess variables;

    protected AbstractListAction(@NotNull String uniqueIdentifier, @NotNull ListVariableAccess variables) {
        super(uniqueIdentifier);
        this.variables = Objects.requireNonNull(variables);
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    /**
     * Placeholders in the value get replaced, but "&amp;" stays as it is, so the entries that get added, set and compared are exactly what
     * got typed in, no matter if the value has placeholders or not.
     */
    @Override
    public boolean hasRawValue() {
        return true;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(LOCALIZATION_KEY_PREFIX + this.getIdentifier());
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable(LOCALIZATION_KEY_PREFIX + this.getIdentifier() + ".desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        // The value gets edited in the window of the action
        return Component.empty();
    }

    @Override
    public abstract @NotNull String getValuePreset();

    /**
     * Creates the window the value gets edited in.
     *
     * @param callback Gets the edited value, or {@code null} if the editing got canceled.
     */
    @NotNull
    protected abstract ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback);

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        PiPWindow[] windowHolder = new PiPWindow[1];
        ListActionValueWindowBody body = this.createValueEditor(Objects.requireNonNullElse(instance.value, this.getValuePreset()), value -> {
            if (handled[0]) return;
            handled[0] = true;
            if (value != null) {
                instance.value = value;
                onEditingCompleted.accept(instance, oldValue, value);
            } else {
                onEditingCanceled.accept(instance);
            }
            if (windowHolder[0] != null) windowHolder[0].close();
        });
        PiPWindow window = new PiPWindow(body.getTitle())
                .setScreen(body)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        windowHolder[0] = window;
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        // A window that gets closed in any other way than with its buttons cancels the editing
        window.addCloseCallback(() -> {
            if (handled[0]) return;
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    /**
     * Reads the index of an action that got its placeholders replaced, and logs a warning if it is no whole number.
     *
     * @return The index, or {@code null} if it is no whole number.
     */
    @Nullable
    protected final Integer parseIndex(@NotNull String listName, @NotNull String index) {
        Integer parsed = VariableLists.parseIndex(index);
        if (parsed == null) this.warn("invalid_index", listName, "[FANCYMENU] Action '{}' didn't change list variable '{}', because its index '{}' is no whole number. The first entry has index 0, and -1 is the last one.", this.getIdentifier(), listName, index);
        return parsed;
    }

    /**
     * Logs a warning if an edit of the list that uses no index didn't work.
     */
    protected final void logFailedEdit(@NotNull String listName, @NotNull ListEditResult result) {
        this.logFailedEdit(listName, result, 0);
    }

    /**
     * Logs a warning if the edit of the list didn't work.
     *
     * @param index The index the edit used, which the warning of an index the list doesn't have shows.
     */
    protected final void logFailedEdit(@NotNull String listName, @NotNull ListEditResult result, int index) {
        switch (result) {
            case NOT_A_LIST -> this.warn("not_a_list", listName, "[FANCYMENU] Action '{}' didn't change variable '{}', because it is a text variable with a value, and list actions never overwrite text. If it should be a list, turn it into one in Manage Variables.", this.getIdentifier(), listName);
            case INDEX_OUT_OF_RANGE -> this.warn("index_out_of_range", listName, "[FANCYMENU] Action '{}' didn't change list variable '{}', because it has no entry at index {}. The first entry has index 0, and -1 is the last one.", this.getIdentifier(), listName, index);
            default -> {
                // Everything else either worked, had nothing to do, or happened while the game shuts down
            }
        }
    }

    private void warn(@NotNull String reason, @NotNull String listName, @NotNull String message, @NotNull Object... arguments) {
        if (!WARNING_COOLDOWN.tryAcquire(this.getIdentifier() + '\n' + reason + '\n' + listName, System.currentTimeMillis())) return;
        LOGGER.warn(message, arguments);
    }

    /**
     * Quotes text of a value for its summary in the script editor (see {@link #getValueSummary(String)}), shortened so the summary stays one
     * short line.
     */
    @NotNull
    static String quoteForSummary(@NotNull String text) {
        String flat = text.replace('\n', ' ').replace('\r', ' ');
        if (flat.length() > SUMMARY_TEXT_LIMIT) flat = flat.substring(0, SUMMARY_TEXT_LIMIT - 1) + "…";
        return "\"" + flat + "\"";
    }

    /**
     * Splits a value into at most the given number of parts. The last part keeps everything after the separator before it, also more separators.
     */
    @NotNull
    static String[] splitValue(@NotNull String value, int maxParts) {
        return VALUE_SEPARATOR_PATTERN.split(value, maxParts);
    }

    /**
     * Reads a boolean part of a value, which can also come from a placeholder.
     *
     * @return The boolean, or the fallback if the part is neither "true" nor "false".
     */
    static boolean parseBoolean(@NotNull String part, boolean fallback) {
        String trimmed = part.strip();
        if ("true".equalsIgnoreCase(trimmed)) return true;
        if ("false".equalsIgnoreCase(trimmed)) return false;
        return fallback;
    }

    /**
     * If an index that got typed in can work: a whole number, or text with placeholders, which only get replaced when the action runs.
     *
     * @param allowEmpty If an empty index is fine, like the one of an entry that gets added at the end.
     */
    public static boolean isValidIndexInput(@NotNull String index, boolean allowEmpty) {
        if (index.isBlank()) return allowEmpty;
        return PlaceholderParser.containsPlaceholders(index) || (VariableLists.parseIndex(index) != null);
    }

    /**
     * Edits the list variables. The actions only edit them through this, so tests can give them their own lists.
     * See {@link VariableHandler} for what the edits do.
     */
    public interface ListVariableAccess {

        @NotNull
        ListEditResult setList(@NotNull String name, @NotNull List<String> entries);

        @NotNull
        ListEditResult addListEntry(@NotNull String name, @NotNull String entry, @Nullable Integer index, boolean skipIfContained, boolean ignoreCase);

        @NotNull
        ListEditResult setListEntry(@NotNull String name, int index, @NotNull String entry);

        @NotNull
        ListEditResult removeListEntryAt(@NotNull String name, int index);

        @NotNull
        ListEditResult removeListEntries(@NotNull String name, @NotNull String entry, boolean allMatches, boolean ignoreCase);

        @NotNull
        ListEditResult clearList(@NotNull String name);

        @NotNull
        ListEditResult arrangeList(@NotNull String name, @NotNull ListArrangement arrangement);

    }

}
