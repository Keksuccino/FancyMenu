package de.keksuccino.fancymenu.customization.listener;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractListener {

    @NotNull
    protected final String identifier;
    protected final Map<String, ListenerInstance> instances = new HashMap<>();

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
        this.instances.put(instance.instanceIdentifier, instance);
    }

    public void unregisterInstance(@NotNull String identifier) {
        this.instances.remove(identifier);
    }

    public void unregisterInstance(@NotNull ListenerInstance instance) {
        String identifier = null;
        for (Map.Entry<String, ListenerInstance> m : this.instances.entrySet()) {
            if (m.getValue() == instance) {
                identifier = m.getKey();
                break;
            }
        }
        if (identifier != null) {
            this.instances.remove(identifier);
        }
    }

    protected void notifyAllInstances() {
        this.instances.forEach((s, instance) -> instance.getActionScript().execute());
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

    @NotNull
    public abstract Component getDisplayName();

    @NotNull
    public abstract List<Component> getDescription();

    public record CustomVariable(@NotNull String name, @NotNull Supplier<String> valueSupplier) {}

}

