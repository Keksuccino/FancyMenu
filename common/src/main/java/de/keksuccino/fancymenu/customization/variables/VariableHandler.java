package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.Legacy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

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
