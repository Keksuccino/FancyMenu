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
 * Owns variable state and persistence. Mutations become visible under {@link #stateLock} immediately, while one
 * revision-aware coordinator serializes debounced atomic database replacements without holding that monitor during
 * serialization or filesystem I/O.
 */
final class VariableStore {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Object stateLock = new Object();
    private final Map<String, Variable> variables = new HashMap<>();
    private final AtomicVariableDatabase database;
    private final VariableUpdateListener updateListener;
    private final SnapshotSerializer snapshotSerializer;
    private final VariablePersistenceCoordinator persistence;

    private long stateRevision;
    private boolean initialized;
    private boolean replacementInProgress;
    private boolean shutdown;

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener) {
        this(database, updateListener, VariableStore::serializeSnapshots, new VariablePersistenceCoordinator.ExecutorScheduler("FancyMenu-VariablePersistence"));
    }

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener, @NotNull SnapshotSerializer snapshotSerializer) {
        this(database, updateListener, snapshotSerializer, new VariablePersistenceCoordinator.ExecutorScheduler("FancyMenu-VariablePersistence"));
    }

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener, @NotNull VariablePersistenceCoordinator.Scheduler scheduler) {
        this(database, updateListener, VariableStore::serializeSnapshots, scheduler);
    }

    VariableStore(@NotNull AtomicVariableDatabase database, @NotNull VariableUpdateListener updateListener, @NotNull SnapshotSerializer snapshotSerializer, @NotNull VariablePersistenceCoordinator.Scheduler scheduler) {
        this.database = Objects.requireNonNull(database);
        this.updateListener = Objects.requireNonNull(updateListener);
        this.snapshotSerializer = Objects.requireNonNull(snapshotSerializer);
        this.persistence = new VariablePersistenceCoordinator(this::capturePersistenceSnapshot, this::writePersistenceSnapshot, Objects.requireNonNull(scheduler));
    }

    void init() {
        ReplacementAdmission admission = this.beginReplacement();
        if (admission == null) return;
        try {
            long revisionToFlushFirst = (admission.initialized() || admission.revision() > 0L) ? admission.revision() : -1L;
            this.persistence.runExclusive(revisionToFlushFirst, "flushing variables before reinitialization", () -> {
                Map<String, Variable> loadedVariables = this.loadReplacementState(false);
                if (loadedVariables == null) return VariablePersistenceCoordinator.ExclusiveCommit.unchanged();
                long revision;
                synchronized (this.stateLock) {
                    this.variables.clear();
                    this.variables.putAll(loadedVariables);
                    for (Variable variable : this.variables.values()) {
                        if (variable.isResetOnLaunchLocked()) variable.setRawValueLocked("");
                    }
                    this.initialized = true;
                    revision = this.nextRevisionLocked();
                }
                return VariablePersistenceCoordinator.ExclusiveCommit.dirty(revision, "initializing variables");
            });
        } finally {
            this.endReplacement();
        }
    }

    void setVariable(@NotNull String name, @Nullable String value) {
        long revision = -1L;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown) return;
            String checkedName = Objects.requireNonNull(name);
            Variable variable = this.variables.get(checkedName);
            boolean created = false;
            if (variable == null) {
                variable = new Variable(checkedName, this);
                this.variables.put(checkedName, variable);
                created = true;
            }
            // Variable is deliberately only a state object; this handler mutation is the single persistence owner.
            String oldValue = variable.getRawValueLocked();
            String encodedValue = Variable.encodeValue(value);
            if (created || !oldValue.equals(encodedValue)) {
                // Publish before dispatch because listener action scripts may immediately read or supersede this assignment.
                variable.setRawValueLocked(encodedValue);
                revision = this.nextRevisionLocked();
            }
            this.updateListener.onVariableUpdated(variable.getName(), oldValue, Objects.requireNonNullElse(value, "0"));
        }
        if (revision >= 0L) this.persistence.markDirty(revision, "setting variable '" + name + "'");
    }

    boolean setVariableIfAbsent(@NotNull String name, @Nullable String value) {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            String checkedName = Objects.requireNonNull(name);
            if (this.shutdown || this.variables.containsKey(checkedName)) return false;
            Variable variable = new Variable(checkedName, this);
            this.variables.put(checkedName, variable);
            variable.setRawValueLocked(Variable.encodeValue(value));
            revision = this.nextRevisionLocked();
            this.updateListener.onVariableUpdated(variable.getName(), "", Objects.requireNonNullElse(value, "0"));
        }
        this.persistence.markDirty(revision, "creating variable '" + name + "'");
        return true;
    }

    @Nullable
    String createVariableWithUniqueCopyName(@NotNull String sourceName, @NotNull String value, boolean resetOnLaunch) {
        String candidate;
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown) return null;
            String baseName = Objects.requireNonNull(sourceName) + "_Copy";
            candidate = baseName;
            int suffix = 2;
            while (this.variables.containsKey(candidate)) {
                candidate = baseName + suffix;
                suffix++;
            }
            Variable variable = new Variable(candidate, this);
            this.variables.put(candidate, variable);
            variable.setRawValueLocked(Variable.encodeValue(value));
            variable.setResetOnLaunchLocked(resetOnLaunch);
            revision = this.nextRevisionLocked();
            this.updateListener.onVariableUpdated(candidate, "", Objects.requireNonNull(value));
        }
        this.persistence.markDirty(revision, "creating copied variable '" + candidate + "'");
        return candidate;
    }

    void replaceVariables(@NotNull List<UserVariableSnapshot> snapshots) {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown) return;
            List<UserVariableSnapshot> stableSnapshots = List.copyOf(Objects.requireNonNull(snapshots));
            Map<String, Variable> previousVariables = new HashMap<>(this.variables);
            try {
                this.variables.clear();
                for (UserVariableSnapshot snapshot : stableSnapshots) {
                    Variable variable = new Variable(snapshot.name(), this);
                    this.variables.put(snapshot.name(), variable);
                    variable.setRawValueLocked(Variable.encodeValue(snapshot.value()));
                    variable.setResetOnLaunchLocked(snapshot.resetOnLaunch());
                    this.updateListener.onVariableUpdated(snapshot.name(), "", snapshot.value());
                }
            } catch (RuntimeException | Error failure) {
                this.variables.clear();
                this.variables.putAll(previousVariables);
                throw failure;
            }
            revision = this.nextRevisionLocked();
        }
        this.persistence.markDirty(revision, "replacing the complete variable state");
    }

    void removeVariable(@NotNull String name) {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown || this.variables.remove(Objects.requireNonNull(name)) == null) return;
            revision = this.nextRevisionLocked();
        }
        this.persistence.markDirty(revision, "removing variable '" + name + "'");
    }

    void clearVariables() {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown || this.variables.isEmpty()) return;
            this.variables.clear();
            revision = this.nextRevisionLocked();
        }
        this.persistence.markDirty(revision, "clearing variables");
    }

    @Nullable
    Variable getVariable(@NotNull String name) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            return this.variables.get(Objects.requireNonNull(name));
        }
    }

    @Nullable
    String getVariableValue(@NotNull String name) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            Variable variable = this.variables.get(Objects.requireNonNull(name));
            return (variable != null) ? variable.getValueLocked() : null;
        }
    }

    @NotNull
    List<Variable> getVariables() {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            return new ArrayList<>(this.variables.values());
        }
    }

    @NotNull
    List<String> getVariableNames() {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            return new ArrayList<>(this.variables.keySet());
        }
    }

    @NotNull
    List<UserVariableSnapshot> getVariableSnapshots() {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            List<UserVariableSnapshot> snapshots = new ArrayList<>(this.variables.size());
            for (Variable variable : this.variables.values()) snapshots.add(variable.createSnapshotLocked());
            return List.copyOf(snapshots);
        }
    }

    boolean variableExists(@NotNull String name) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            return this.variables.containsKey(Objects.requireNonNull(name));
        }
    }

    @NotNull
    String getValue(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            return variable.getValueLocked();
        }
    }

    void setValue(@NotNull Variable variable, @Nullable String value) {
        long revision = -1L;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            if (this.shutdown) return;
            String encodedValue = Variable.encodeValue(value);
            if (variable.getRawValueLocked().equals(encodedValue)) return;
            variable.setRawValueLocked(encodedValue);
            if (this.variables.get(variable.getName()) == variable) revision = this.nextRevisionLocked();
        }
        if (revision >= 0L) this.persistence.markDirty(revision, "updating variable '" + variable.getName() + "'");
    }

    boolean isResetOnLaunch(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            return variable.isResetOnLaunchLocked();
        }
    }

    @NotNull
    UserVariableSnapshot getSnapshot(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            return variable.createSnapshotLocked();
        }
    }

    void setResetOnLaunch(@NotNull Variable variable, boolean resetOnLaunch) {
        long revision = -1L;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            if (this.shutdown || variable.isResetOnLaunchLocked() == resetOnLaunch) return;
            variable.setResetOnLaunchLocked(resetOnLaunch);
            if (this.variables.get(variable.getName()) == variable) revision = this.nextRevisionLocked();
        }
        if (revision >= 0L) this.persistence.markDirty(revision, "updating reset-on-launch for variable '" + variable.getName() + "'");
    }

    void toggleResetOnLaunch(@NotNull Variable variable) {
        long revision = -1L;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            if (this.shutdown) return;
            variable.setResetOnLaunchLocked(!variable.isResetOnLaunchLocked());
            if (this.variables.get(variable.getName()) == variable) revision = this.nextRevisionLocked();
        }
        if (revision >= 0L) this.persistence.markDirty(revision, "toggling reset-on-launch for variable '" + variable.getName() + "'");
    }

    @NotNull
    PropertyContainer serialize(@NotNull Variable variable) {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            this.requireOwner(variable);
            return variable.serializeLocked();
        }
    }

    boolean flush() {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            revision = this.stateRevision;
        }
        return this.persistence.flush(revision, "flushing variables");
    }

    void writeToFile() {
        this.flush();
    }

    void readFromFile() {
        this.replaceFromDatabase(false);
    }

    @Legacy("This reads variables from v2 variable files. Remove this in the future.")
    void readFromLegacyFile() {
        this.replaceFromDatabase(true);
    }

    /**
     * Closes mutation admission before capturing the shutdown revision. The explicit flush therefore includes an
     * admitted mutation even when its out-of-lock dirty notification has not reached the coordinator yet.
     */
    void shutdown() {
        long revision;
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown) return;
            this.shutdown = true;
            revision = this.stateRevision;
        }
        if (!this.persistence.shutdown(revision, "shutting down variables")) {
            LOGGER.error("[FANCYMENU] Failed to durably flush the latest variable revision while shutting down.");
        }
    }

    private void replaceFromDatabase(boolean requireLegacy) {
        ReplacementAdmission admission = this.beginReplacement();
        if (admission == null) return;
        try {
            this.persistence.runExclusive(admission.revision(), "flushing variables before database reload", () -> {
                Map<String, Variable> loadedVariables = this.loadReplacementState(requireLegacy);
                if (loadedVariables == null) return VariablePersistenceCoordinator.ExclusiveCommit.unchanged();
                long revision;
                synchronized (this.stateLock) {
                    this.variables.clear();
                    this.variables.putAll(loadedVariables);
                    revision = this.nextRevisionLocked();
                }
                return VariablePersistenceCoordinator.ExclusiveCommit.clean(revision);
            });
        } finally {
            this.endReplacement();
        }
    }

    /**
     * Establishes reload call ordering under the same monitor as mutations. Calls admitted before this gate are
     * flushed before the read; calls arriving after it wait and linearize after the replacement has been published.
     */
    @Nullable
    private ReplacementAdmission beginReplacement() {
        synchronized (this.stateLock) {
            this.awaitReplacementLocked();
            if (this.shutdown) return null;
            this.replacementInProgress = true;
            return new ReplacementAdmission(this.stateRevision, this.initialized);
        }
    }

    private void endReplacement() {
        synchronized (this.stateLock) {
            this.replacementInProgress = false;
            this.stateLock.notifyAll();
        }
    }

    private void awaitReplacementLocked() {
        boolean interrupted = false;
        while (this.replacementInProgress) {
            try {
                this.stateLock.wait();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private long nextRevisionLocked() {
        this.stateRevision++;
        return this.stateRevision;
    }

    private void requireOwner(@NotNull Variable variable) {
        if (variable.getOwner() != this) throw new IllegalArgumentException("Variable belongs to a different variable store: " + variable.getName());
    }

    @Nullable
    private Map<String, Variable> loadReplacementState(boolean requireLegacy) {
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
            return legacy ? this.deserializeLegacyVariables(set) : this.deserializeVariables(set);
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
    private Map<String, Variable> deserializeVariables(@NotNull PropertyContainerSet set) {
        Map<String, Variable> loadedVariables = new HashMap<>();
        for (PropertyContainer container : set.getContainersOfType("variable")) {
            Variable variable = Variable.deserialize(container, this);
            if (variable != null) loadedVariables.put(variable.getName(), variable);
        }
        return loadedVariables;
    }

    @NotNull
    private Map<String, Variable> deserializeLegacyVariables(@NotNull PropertyContainerSet set) {
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

    @NotNull
    private VariablePersistenceCoordinator.PersistenceSnapshot capturePersistenceSnapshot() {
        long revision;
        List<StoredVariableSnapshot> snapshots;
        synchronized (this.stateLock) {
            revision = this.stateRevision;
            snapshots = new ArrayList<>(this.variables.size());
            for (Variable variable : this.variables.values()) snapshots.add(new StoredVariableSnapshot(variable.getName(), variable.getRawValueLocked(), variable.isResetOnLaunchLocked()));
        }
        return new VariablePersistenceCoordinator.PersistenceSnapshot(revision, this.snapshotSerializer.serialize(List.copyOf(snapshots)));
    }

    private boolean writePersistenceSnapshot(@NotNull VariablePersistenceCoordinator.PersistenceSnapshot snapshot, @NotNull String operation) {
        try {
            this.database.write(snapshot.serializedVariables());
            return true;
        } catch (Exception ex) {
            if (ex instanceof AtomicVariableDatabase.TemporaryFileCleanupException cleanupException && cleanupException.replacementCompleted()) {
                LOGGER.error("[FANCYMENU] Replaced variable database '{}' while {}, but failed to clean its exact temporary file.", this.database.getTarget(), operation, ex);
                return true;
            }
            LOGGER.error("[FANCYMENU] Failed while {} in variable database '{}'. The previous complete database file was preserved because replacement did not finish.", operation, this.database.getTarget(), ex);
            return false;
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

        @NotNull String serialize(@NotNull List<StoredVariableSnapshot> snapshots);

    }

    record StoredVariableSnapshot(@NotNull String name, @NotNull String rawValue, boolean resetOnLaunch) {
    }

    private record ReplacementAdmission(long revision, boolean initialized) {
    }

}
