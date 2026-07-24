package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes every variable observer, mutation, and persistence transaction through one reentrant monitor. Keeping
 * disk replacement inside the same boundary prevents an older snapshot from overtaking a newer mutation on disk.
 */
final class VariableStore {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Object stateLock = new Object();
    private final Map<String, Variable> variables = new HashMap<>();
    private final AtomicVariableDatabase database;
    private final VariableUpdateListener updateListener;
    private final SnapshotSerializer snapshotSerializer;
    private boolean shutdown;
    private int persistenceDeferralDepth;

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener) {
        this(database, updateListener, VariableStore::serializeSnapshots);
    }

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener, @NotNull SnapshotSerializer snapshotSerializer) {
        this.database = Objects.requireNonNull(database);
        this.updateListener = Objects.requireNonNull(updateListener);
        this.snapshotSerializer = Objects.requireNonNull(snapshotSerializer);
    }

    void init() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            Map<String, Variable> loadedVariables = this.loadReplacementStateLocked(false);
            if (loadedVariables == null) return;
            this.variables.clear();
            this.variables.putAll(loadedVariables);
            for (Variable variable : this.variables.values()) {
                if (variable.isResetOnLaunchLocked()) variable.setRawValueLocked("");
            }
            this.persistLocked("initializing variables");
        }
    }

    void setVariable(@NotNull String name, @Nullable String value) {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            Variable variable = this.variables.get(Objects.requireNonNull(name));
            if (variable == null) {
                variable = new Variable(name, this);
                this.variables.put(name, variable);
            }
            this.updateListener.onVariableUpdated(variable.getName(), variable.getRawValueLocked(), Objects.requireNonNullElse(value, "0"));
            variable.setValue(value);
            // This second write is intentionally retained until the separate write-coalescing QA item is addressed.
            this.persistLocked("setting variable '" + name + "'");
        }
    }

    boolean setVariableIfAbsent(@NotNull String name, @Nullable String value) {
        synchronized (this.stateLock) {
            if (this.shutdown || this.variables.containsKey(Objects.requireNonNull(name))) return false;
            this.setVariable(name, value);
            return true;
        }
    }

    @Nullable
    String createVariableWithUniqueCopyName(@NotNull String sourceName, @NotNull String value, boolean resetOnLaunch) {
        synchronized (this.stateLock) {
            if (this.shutdown) return null;
            String baseName = Objects.requireNonNull(sourceName) + "_Copy";
            String candidate = baseName;
            int suffix = 2;
            while (this.variables.containsKey(candidate)) {
                candidate = baseName + suffix;
                suffix++;
            }
            this.setVariable(candidate, Objects.requireNonNull(value));
            Variable variable = this.variables.get(candidate);
            if (variable != null) variable.setResetOnLaunch(resetOnLaunch);
            return candidate;
        }
    }

    void replaceVariables(@NotNull List<UserVariableSnapshot> snapshots) {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            List<UserVariableSnapshot> stableSnapshots = List.copyOf(Objects.requireNonNull(snapshots));
            Map<String, Variable> previousVariables = new HashMap<>(this.variables);
            this.persistenceDeferralDepth++;
            try {
                this.variables.clear();
                for (UserVariableSnapshot snapshot : stableSnapshots) {
                    Variable variable = new Variable(snapshot.name(), this);
                    this.variables.put(snapshot.name(), variable);
                    this.updateListener.onVariableUpdated(snapshot.name(), variable.getRawValueLocked(), snapshot.value());
                    variable.setRawValueLocked(Variable.encodeValue(snapshot.value()));
                    variable.setResetOnLaunchLocked(snapshot.resetOnLaunch());
                }
            } catch (RuntimeException | Error failure) {
                this.variables.clear();
                this.variables.putAll(previousVariables);
                throw failure;
            } finally {
                this.persistenceDeferralDepth--;
            }
            this.persistLocked("replacing the complete variable state");
        }
    }

    void removeVariable(@NotNull String name) {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            this.variables.remove(Objects.requireNonNull(name));
            this.persistLocked("removing variable '" + name + "'");
        }
    }

    void clearVariables() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            this.variables.clear();
            this.persistLocked("clearing variables");
        }
    }

    @Nullable
    Variable getVariable(@NotNull String name) {
        synchronized (this.stateLock) {
            return this.variables.get(Objects.requireNonNull(name));
        }
    }

    @Nullable
    String getVariableValue(@NotNull String name) {
        synchronized (this.stateLock) {
            Variable variable = this.variables.get(Objects.requireNonNull(name));
            return (variable != null) ? variable.getValueLocked() : null;
        }
    }

    @NotNull
    List<Variable> getVariables() {
        synchronized (this.stateLock) {
            return new ArrayList<>(this.variables.values());
        }
    }

    @NotNull
    List<String> getVariableNames() {
        synchronized (this.stateLock) {
            return new ArrayList<>(this.variables.keySet());
        }
    }

    @NotNull
    List<UserVariableSnapshot> getVariableSnapshots() {
        synchronized (this.stateLock) {
            List<UserVariableSnapshot> snapshots = new ArrayList<>(this.variables.size());
            for (Variable variable : this.variables.values()) {
                snapshots.add(variable.createSnapshotLocked());
            }
            return List.copyOf(snapshots);
        }
    }

    boolean variableExists(@NotNull String name) {
        synchronized (this.stateLock) {
            return this.variables.containsKey(Objects.requireNonNull(name));
        }
    }

    @NotNull
    String getValue(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            return variable.getValueLocked();
        }
    }

    void setValue(@NotNull Variable variable, @Nullable String value) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            if (this.shutdown) return;
            variable.setRawValueLocked(Variable.encodeValue(value));
            this.persistLocked("updating variable '" + variable.getName() + "'");
        }
    }

    boolean isResetOnLaunch(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            return variable.isResetOnLaunchLocked();
        }
    }

    @NotNull
    UserVariableSnapshot getSnapshot(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            return variable.createSnapshotLocked();
        }
    }

    void setResetOnLaunch(@NotNull Variable variable, boolean resetOnLaunch) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            if (this.shutdown) return;
            variable.setResetOnLaunchLocked(resetOnLaunch);
            this.persistLocked("updating reset-on-launch for variable '" + variable.getName() + "'");
        }
    }

    void toggleResetOnLaunch(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            if (this.shutdown) return;
            variable.setResetOnLaunchLocked(!variable.isResetOnLaunchLocked());
            this.persistLocked("toggling reset-on-launch for variable '" + variable.getName() + "'");
        }
    }

    @NotNull
    PropertyContainer serialize(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.requireOwner(variable);
            return variable.serializeLocked();
        }
    }

    void writeToFile() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            this.persistLocked("flushing variables");
        }
    }

    void readFromFile() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            Map<String, Variable> loadedVariables = this.loadReplacementStateLocked(false);
            if (loadedVariables == null) return;
            this.variables.clear();
            this.variables.putAll(loadedVariables);
        }
    }

    @Legacy("This reads variables from v2 variable files. Remove this in the future.")
    void readFromLegacyFile() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            Map<String, Variable> loadedVariables = this.loadReplacementStateLocked(true);
            if (loadedVariables == null) return;
            this.variables.clear();
            this.variables.putAll(loadedVariables);
        }
    }

    /**
     * Closes mutation admission before the final flush. Late daemon-ticker mutations deterministically become no-ops,
     * avoiding both post-flush writes and repeated shutdown log noise.
     */
    void shutdown() {
        synchronized (this.stateLock) {
            if (this.shutdown) return;
            this.shutdown = true;
            this.persistLocked("shutting down variables");
        }
    }

    private void requireOwner(@NotNull Variable variable) {
        if (variable.getOwner() != this) {
            throw new IllegalArgumentException("Variable belongs to a different variable store: " + variable.getName());
        }
    }

    @Nullable
    private Map<String, Variable> loadReplacementStateLocked(boolean requireLegacy) {
        try {
            String serializedVariables = this.database.read();
            if (!isStructurallyComplete(serializedVariables)) {
                LOGGER.error("[FANCYMENU] Variable database '{}' is incomplete; keeping the current in-memory state and file unchanged.", this.database.getTarget());
                return null;
            }
            PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(serializedVariables);
            if (set == null) {
                LOGGER.error("[FANCYMENU] Variable database '{}' is malformed; keeping the current in-memory state and file unchanged.", this.database.getTarget());
                return null;
            }
            boolean legacy = "cached_variables".equals(set.getType());
            boolean current = "user_variables".equals(set.getType());
            if (!legacy && !current) {
                LOGGER.error("[FANCYMENU] Variable database '{}' has unexpected type '{}'; keeping the current in-memory state and file unchanged.", this.database.getTarget(), set.getType());
                return null;
            }
            if (requireLegacy && !legacy) {
                LOGGER.error("[FANCYMENU] Variable database '{}' is not a legacy cached_variables database; keeping the current in-memory state unchanged.", this.database.getTarget());
                return null;
            }
            return legacy ? this.deserializeLegacyVariablesLocked(set) : this.deserializeVariablesLocked(set);
        } catch (NoSuchFileException ex) {
            return requireLegacy ? null : new HashMap<>();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to completely read variable database '{}'; keeping the current in-memory state and file unchanged.", this.database.getTarget(), ex);
            return null;
        }
    }

    private static boolean isStructurallyComplete(@NotNull String serializedVariables) {
        boolean foundType = false;
        boolean insideContainer = false;
        for (String line : serializedVariables.replace("\r", "\n").split("\n", -1)) {
            String compactLine = line.replaceAll("[\\p{Z}\\s]+", "");
            if (!insideContainer && compactLine.startsWith("type=") && compactLine.length() > "type=".length()) foundType = true;
            if (compactLine.endsWith("{")) {
                if (insideContainer) return false;
                insideContainer = true;
            } else if (compactLine.startsWith("}")) {
                if (!insideContainer) return false;
                insideContainer = false;
            }
        }
        return foundType && !insideContainer;
    }

    @NotNull
    private Map<String, Variable> deserializeVariablesLocked(@NotNull PropertyContainerSet set) {
        Map<String, Variable> loadedVariables = new HashMap<>();
        for (PropertyContainer container : set.getContainersOfType("variable")) {
            Variable variable = Variable.deserialize(container, this);
            if (variable != null) loadedVariables.put(variable.getName(), variable);
        }
        return loadedVariables;
    }

    @NotNull
    private Map<String, Variable> deserializeLegacyVariablesLocked(@NotNull PropertyContainerSet set) {
        Map<String, Variable> loadedVariables = new HashMap<>();
        List<PropertyContainer> containers = set.getContainersOfType("variables");
        if (containers.isEmpty()) return loadedVariables;
        for (Map.Entry<String, String> entry : containers.get(0).getProperties().entrySet()) {
            Variable variable = new Variable(entry.getKey(), this);
            variable.setRawValueLocked(entry.getValue());
            loadedVariables.put(entry.getKey(), variable);
        }
        return loadedVariables;
    }

    void persistLocked(@NotNull String operation) {
        if (this.persistenceDeferralDepth > 0) return;
        try {
            List<StoredVariableSnapshot> snapshots = new ArrayList<>(this.variables.size());
            for (Variable variable : this.variables.values()) {
                snapshots.add(new StoredVariableSnapshot(variable.getName(), variable.getRawValueLocked(), variable.isResetOnLaunchLocked()));
            }
            this.database.write(this.snapshotSerializer.serialize(List.copyOf(snapshots)));
        } catch (Exception ex) {
            if (ex instanceof AtomicVariableDatabase.TemporaryFileCleanupException cleanupException && cleanupException.replacementCompleted()) {
                LOGGER.error("[FANCYMENU] Replaced variable database '{}' while {}, but failed to clean its exact temporary file.", this.database.getTarget(), operation, ex);
            } else {
                LOGGER.error("[FANCYMENU] Failed while {} in variable database '{}'. The previous complete database file was preserved because replacement did not finish.", operation, this.database.getTarget(), ex);
            }
        }
    }

    @NotNull
    private static String serializeSnapshots(@NotNull List<StoredVariableSnapshot> snapshots) {
        PropertyContainerSet set = new PropertyContainerSet("user_variables");
        for (StoredVariableSnapshot snapshot : snapshots) {
            PropertyContainer container = new PropertyContainer("variable");
            container.putProperty("name", snapshot.name());
            container.putProperty("value", snapshot.rawValue());
            container.putProperty("reset_on_launch", Boolean.toString(snapshot.resetOnLaunch()));
            set.putContainer(container);
        }
        return PropertiesParser.serializeSetToFancyString(set);
    }

    @FunctionalInterface
    interface VariableUpdateListener {

        void onVariableUpdated(@NotNull String name, @NotNull String oldValue, @NotNull String newValue);

    }

    @FunctionalInterface
    interface SnapshotSerializer {

        @NotNull
        String serialize(@NotNull List<StoredVariableSnapshot> snapshots);

    }

    record StoredVariableSnapshot(@NotNull String name, @NotNull String rawValue, boolean resetOnLaunch) {
    }

}
