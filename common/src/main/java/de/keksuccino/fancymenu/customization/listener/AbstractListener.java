package de.keksuccino.fancymenu.customization.listener;

import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected final String identifier;
    private final Object instanceLock = new Object();
    private final Map<String, ListenerInstance> instances = new HashMap<>();
    private volatile InstanceState instanceState = new InstanceState(List.of(), 0L);

    public AbstractListener(@NotNull String identifier) {
        this.identifier = identifier;
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public ListenerInstance createFreshInstance() {
        ListenerInstance listener = new ListenerInstance(this);
        this.registerCustomVariablesToInstance(listener);
        return listener;
    }

    public void registerInstance(@NotNull ListenerInstance instance) {
        this.validateParent(instance);
        synchronized (this.instanceLock) {
            boolean wasActive = !this.instances.isEmpty();
            ListenerInstance previous = this.instances.put(instance.instanceIdentifier, instance);
            if (previous != instance) {
                this.publishInstanceState(wasActive);
            }
        }
    }

    public void unregisterInstance(@NotNull String identifier) {
        synchronized (this.instanceLock) {
            boolean wasActive = !this.instances.isEmpty();
            if (this.instances.remove(identifier) != null) {
                this.publishInstanceState(wasActive);
            }
        }
    }

    public void unregisterInstance(@NotNull ListenerInstance instance) {
        synchronized (this.instanceLock) {
            boolean wasActive = !this.instances.isEmpty();
            if (this.instances.entrySet().removeIf(entry -> entry.getValue() == instance)) {
                this.publishInstanceState(wasActive);
            }
        }
    }

    public final boolean hasInstancesListening() {
        return !this.instanceState.instances.isEmpty();
    }

    public final long getActiveInstanceRevision() {
        InstanceState state = this.instanceState;
        return state.instances.isEmpty() ? -1L : state.revision;
    }

    public final boolean isActiveAtRevision(long revision) {
        InstanceState state = this.instanceState;
        return state.revision == revision && !state.instances.isEmpty();
    }

    public final void replaceInstances(@NotNull Collection<ListenerInstance> replacements) {
        Map<String, ListenerInstance> validatedReplacements = new HashMap<>();
        for (ListenerInstance instance : replacements) {
            this.validateParent(instance);
            validatedReplacements.put(instance.instanceIdentifier, instance);
        }
        synchronized (this.instanceLock) {
            if (this.instances.equals(validatedReplacements)) {
                return;
            }
            boolean wasActive = !this.instances.isEmpty();
            this.instances.clear();
            this.instances.putAll(validatedReplacements);
            this.publishInstanceState(wasActive);
        }
    }

    protected final void notifyAllInstances() {
        List<ListenerInstance> instancesAtDispatch = this.instanceState.instances;
        for (ListenerInstance instance : instancesAtDispatch) {
            try {
                instance.getActionScript().execute();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Error while trying to execute action script of listener instance!", ex);
            }
        }
    }

    /**
     * Called exactly once when this provider transitions from zero registered instances to at least one.
     * Implementations must remain local to this provider and must not query or mutate another provider while the lifecycle lock is held.
     */
    protected void onActivated() {
    }

    /**
     * Called exactly once when this provider transitions from at least one registered instance to zero.
     * Implementations must remain local to this provider and must not query or mutate another provider while the lifecycle lock is held.
     */
    protected void onDeactivated() {
    }

    protected abstract void buildCustomVariablesAndAddToList(List<CustomVariable> list);

    @NotNull
    public List<CustomVariable> getCustomVariables() {
        List<CustomVariable> variables = new ArrayList<>();
        this.buildCustomVariablesAndAddToList(variables);
        return variables;
    }

    protected void registerCustomVariablesToInstance(@NotNull ListenerInstance instance) {
        for (CustomVariable v : this.getCustomVariables()) {
            instance.getActionScript().addValuePlaceholder(v.name(), v.valueSupplier());
        }
    }

    private void validateParent(@NotNull ListenerInstance instance) {
        if (instance.parent != this) {
            throw new IllegalArgumentException("Tried to register listener instance with the wrong provider: " + instance.instanceIdentifier);
        }
    }

    /**
     * Publishes the immutable dispatch snapshot before lifecycle callbacks run so lock-free readers always observe the state represented by the callback.
     */
    private void publishInstanceState(boolean wasActive) {
        InstanceState previousState = this.instanceState;
        this.instanceState = new InstanceState(List.copyOf(this.instances.values()), previousState.revision + 1L);
        boolean isActive = !this.instanceState.instances.isEmpty();
        if (!wasActive && isActive) {
            this.onActivated();
        } else if (wasActive && !isActive) {
            this.onDeactivated();
        }
    }

    @NotNull
    public abstract Component getDisplayName();

    @NotNull
    public abstract List<Component> getDescription();

    private record InstanceState(@NotNull List<ListenerInstance> instances, long revision) {}

    public record CustomVariable(@NotNull String name, @NotNull Supplier<String> valueSupplier) {}

}
