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

public final class VariableHandler {

    protected static final File VARIABLES_FILE = new File(FancyMenu.MOD_DIR.getPath() + "/user_variables.db");
    private static final VariableStore STORE = new VariableStore(new AtomicVariableDatabase(VARIABLES_FILE.toPath()), VariableHandler::notifyVariableUpdated);

    private VariableHandler() {
    }

    public static void init() {
        STORE.init();
    }

    public static void setVariable(@NotNull String name, @Nullable String value) {
        STORE.setVariable(name, value);
    }

    @NotNull
    public static String normalizeVariableReferenceName(@NotNull String name) {
        return Objects.requireNonNull(name).strip();
    }

    public static boolean setVariableIfAbsent(@NotNull String name, @Nullable String value) {
        return STORE.setVariableIfAbsent(name, value);
    }

    @Nullable
    public static String createVariableWithUniqueCopyName(@NotNull String sourceName, @NotNull String value, boolean resetOnLaunch) {
        return STORE.createVariableWithUniqueCopyName(sourceName, value, resetOnLaunch);
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
