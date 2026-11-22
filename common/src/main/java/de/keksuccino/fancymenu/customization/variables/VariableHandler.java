package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import de.keksuccino.fancymenu.util.Legacy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class VariableHandler {

    protected static final File VARIABLES_FILE = new File(FancyMenu.MOD_DIR.getPath() + "/user_variables.db");
    private static final VariableStore STORE = new VariableStore(new AtomicVariableDatabase(VARIABLES_FILE.toPath()), new VariableStore.VariableUpdateListener() {
        @Override
        public void onVariableUpdated(@NotNull String name, @NotNull String oldValue, @NotNull String newValue) {
            notifyVariableUpdated(name, oldValue, newValue);
        }
        @Override
        public boolean isListening() {
            return Listeners.ON_VARIABLE_UPDATED.hasInstancesListening();
        }
    });
    private static final Random LIST_SHUFFLE_RANDOM = new Random();

    private VariableHandler() {
    }

    public static void init() {
        STORE.init();
    }

    public static void setVariable(@NotNull String name, @Nullable String value) {
        STORE.setVariable(name, value);
    }

    /**
     * Normalizes a variable name that got typed in or comes from placeholders, see {@link Variable#normalizeReferenceName(String)}.
     */
    @NotNull
    public static String normalizeVariableReferenceName(@NotNull String name) {
        return Variable.normalizeReferenceName(name);
    }

    public static boolean setVariableIfAbsent(@NotNull String name, @Nullable String value) {
        return STORE.setVariableIfAbsent(name, value);
    }

    @Nullable
    public static String createVariableWithUniqueCopyName(@NotNull String sourceName, @NotNull String value, boolean resetOnLaunch) {
        return STORE.createVariableWithUniqueCopyName(sourceName, value, resetOnLaunch);
    }

    /**
     * Creates a copy of the variable the snapshot describes, with its type, value and settings, named after it with a {@code _Copy} suffix.
     *
     * @return The name of the copy, or {@code null} if the variables are shut down.
     */
    @Nullable
    public static String createVariableWithUniqueCopyName(@NotNull UserVariableSnapshot source) {
        return STORE.createVariableWithUniqueCopyName(source);
    }

    /**
     * Creates a list variable with the entries, if no variable has the name yet.
     *
     * @return If the variable got created.
     */
    public static boolean setListIfAbsent(@NotNull String name, @NotNull List<String> entries) {
        return STORE.setListIfAbsent(name, List.copyOf(entries));
    }

    /**
     * Replaces all entries of a list variable.
     *
     * <p>All list edits work like this: a missing variable gets created as a list, and an empty text variable becomes one. A text variable
     * with a value stays as it is ({@link ListEditResult#NOT_A_LIST}), so list edits never overwrite text. Indexes start at 0, and negative
     * ones count from the end (-1 is the last entry), see {@link VariableLists}.</p>
     */
    @NotNull
    public static ListEditResult setList(@NotNull String name, @NotNull List<String> entries) {
        List<String> newEntries = List.copyOf(entries);
        return STORE.editList(name, list -> VariableLists.replaceAll(list, newEntries));
    }

    /**
     * Adds an entry to a list variable, at the end or at the index (see {@link VariableLists#resolveInsertIndex(int, int)}).
     * See {@link #setList(String, List)} for how list edits treat missing and text variables.
     *
     * @param index The index the new entry gets, or {@code null} to add it at the end.
     * @param skipIfContained If nothing gets added when the list already has an entry with the value.
     */
    @NotNull
    public static ListEditResult addListEntry(@NotNull String name, @NotNull String entry, @Nullable Integer index, boolean skipIfContained, boolean ignoreCase) {
        Objects.requireNonNull(entry);
        return STORE.editList(name, list -> VariableLists.add(list, entry, index, skipIfContained, ignoreCase));
    }

    /**
     * Replaces the entry at the index of a list variable. See {@link #setList(String, List)} for how list edits treat missing and text variables.
     */
    @NotNull
    public static ListEditResult setListEntry(@NotNull String name, int index, @NotNull String entry) {
        Objects.requireNonNull(entry);
        return STORE.editList(name, list -> VariableLists.set(list, index, entry));
    }

    /**
     * Removes the entry at the index of a list variable. See {@link #setList(String, List)} for how list edits treat missing and text variables.
     */
    @NotNull
    public static ListEditResult removeListEntryAt(@NotNull String name, int index) {
        return STORE.editList(name, list -> VariableLists.removeAt(list, index));
    }

    /**
     * Removes the first entry of a list variable that matches the value, or all of them. See {@link #setList(String, List)} for how list edits
     * treat missing and text variables.
     */
    @NotNull
    public static ListEditResult removeListEntries(@NotNull String name, @NotNull String entry, boolean allMatches, boolean ignoreCase) {
        Objects.requireNonNull(entry);
        return STORE.editList(name, list -> VariableLists.remove(list, entry, allMatches, ignoreCase));
    }

    /**
     * Removes all entries of a list variable. See {@link #setList(String, List)} for how list edits treat missing and text variables.
     */
    @NotNull
    public static ListEditResult clearList(@NotNull String name) {
        return STORE.editList(name, VariableLists::clear);
    }

    /**
     * Sorts, reverses, shuffles or removes the duplicates of a list variable. See {@link #setList(String, List)} for how list edits treat
     * missing and text variables.
     */
    @NotNull
    public static ListEditResult arrangeList(@NotNull String name, @NotNull ListArrangement arrangement) {
        Objects.requireNonNull(arrangement);
        return STORE.editList(name, list -> VariableLists.arrange(list, arrangement, LIST_SHUFFLE_RANDOM));
    }

    /**
     * Returns the value for things that show and edit one single value, like input fields and checkboxes. Text variables give their text.
     * Lists give the text that assigning gives the same entries again (see {@link VariableLists#toSingleValueText(List)}), so a field linked
     * to a list shows what it wrote into it, instead of the JSON text form.
     *
     * @return The value, or {@code null} if the variable doesn't exist.
     */
    @Nullable
    public static String getSingleValueText(@NotNull String name) {
        List<String> entries = STORE.getListEntries(name);
        return (entries != null) ? VariableLists.toSingleValueText(entries) : STORE.getVariableValue(name);
    }

    /**
     * Returns the entries of a list variable. The variables keep this immutable copy until the list changes, so reading a list every frame
     * copies nothing.
     *
     * @return The entries, or {@code null} if the variable doesn't exist or is a text variable.
     */
    @Nullable
    public static List<String> getListEntries(@NotNull String name) {
        return STORE.getListEntries(name);
    }

    /**
     * @return The type of the variable, or {@code null} if it doesn't exist.
     */
    @Nullable
    public static VariableType getVariableType(@NotNull String name) {
        return STORE.getVariableType(name);
    }

    public static boolean isListVariable(@NotNull String name) {
        return STORE.getVariableType(name) == VariableType.LIST;
    }

    /**
     * Makes an existing variable a list with the entries, also when it is a text variable with a value. Only for conversions the user
     * explicitly asks for, everything else should use the list edits, which never overwrite text.
     *
     * @return If the variable exists.
     */
    public static boolean convertToList(@NotNull String name, @NotNull List<String> entries) {
        return STORE.convertToList(name, List.copyOf(entries));
    }

    /**
     * Makes an existing variable a text variable with the value, also when it is a list. Only for conversions the user explicitly asks for.
     *
     * @return If the variable exists.
     */
    public static boolean convertToText(@NotNull String name, @NotNull String value) {
        return STORE.convertToText(name, Objects.requireNonNull(value));
    }

    public static void replaceVariables(@NotNull List<UserVariableSnapshot> snapshots) {
        STORE.replaceVariables(snapshots);
    }

    public static void removeVariable(@NotNull String name) {
        STORE.removeVariable(name);
    }

    @Nullable
    public static Variable getVariable(@NotNull String name) {
        return STORE.getVariable(name);
    }

    /**
     * Returns the text form of the variable. For lists, it is a JSON array of the entries, see {@link VariableLists#toText(List)}.
     *
     * @return The text form, or {@code null} if the variable doesn't exist.
     */
    @Nullable
    public static String getVariableValue(@NotNull String name) {
        return STORE.getVariableValue(name);
    }

    @NotNull
    public static List<Variable> getVariables() {
        return STORE.getVariables();
    }

    @NotNull
    public static List<UserVariableSnapshot> getVariableSnapshots() {
        return STORE.getVariableSnapshots();
    }

    @NotNull
    public static List<String> getVariableNames() {
        return STORE.getVariableNames();
    }

    /**
     * Returns the names of all variables of the type, for example to suggest only list variables in name fields.
     */
    @NotNull
    public static List<String> getVariableNames(@NotNull VariableType type) {
        return STORE.getVariableNames(type);
    }

    /**
     * Returns the names of FancyMenu's variables as they are right now, as a lookup that finds the longest name that starts at a
     * {@code $$} of a text. Unlike {@link #getVariableNames()}, it copies nothing: it is the name index the variables keep for resolving
     * references, which only gets built again after a variable got added or removed. It never changes, so later changes of the variables
     * don't show up in it, and using it never blocks the variables.
     */
    @NotNull
    public static ShortReferenceResolver.NameLookup getVariableNameLookup() {
        return STORE.getNameIndex();
    }

    /**
     * Replaces every {@code $$variable_name} reference to one of FancyMenu's variables with the value of the variable.
     * At every {@code $$}, the variable with the longest name that starts there wins, and all references of the text get the values of the same moment.
     */
    @NotNull
    public static String resolveVariableReferences(@NotNull String text) {
        return STORE.resolveReferences(text);
    }

    /**
     * Returns a number that increases whenever FancyMenu's variables change: when a variable gets added, removed or replaced, or gets a new
     * value or setting. Code that shows what {@code $$name} references resolve to can compare it once per frame, to find out if it has to
     * resolve them again.
     *
     * <p>Reading it never blocks and costs about as much as reading a field. Read it before resolving the references, so a change while
     * they get resolved still shows up as a new number the next time.</p>
     */
    public static long getRevision() {
        return STORE.getRevision();
    }

    public static void clearVariables() {
        STORE.clearVariables();
    }

    public static boolean variableExists(@NotNull String name) {
        return STORE.variableExists(name);
    }

    /** Blocks until the latest admitted variable revision has completed one durable write attempt. */
    public static boolean flush() {
        return STORE.flush();
    }

    protected static void writeToFile() {
        flush();
    }

    protected static void readFromFile() {
        STORE.readFromFile();
    }

    @Legacy("This reads variables from v2 variable files. Remove this in the future.")
    protected static void readFromLegacyFile() {
        STORE.readFromLegacyFile();
    }

    /**
     * Flushes a final complete snapshot and then rejects late daemon-thread mutations for the remainder of shutdown.
     * Repeated calls are safe and do not perform duplicate writes.
     */
    public static void shutdown() {
        STORE.shutdown();
    }

    static VariableStore getStore() {
        return STORE;
    }

    private static void notifyVariableUpdated(@NotNull String name, @NotNull String oldValue, @NotNull String newValue) {
        if (Listeners.ON_VARIABLE_UPDATED.hasInstancesListening()) {
            Listeners.ON_VARIABLE_UPDATED.onVariableUpdated(name, oldValue, newValue);
        }
    }

}
