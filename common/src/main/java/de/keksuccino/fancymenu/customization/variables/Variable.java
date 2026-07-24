package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Variable {

    private static final String NEWLINE_REPLACEMENT = "+*||<FM_NEWLINE>||*+";

    @NotNull
    private final String name;
    @NotNull
    private String value = "";
    private boolean resetOnLaunch;
    @NotNull
    private final VariableStore owner;

    public Variable(@NotNull String name) {
        this(name, VariableHandler.getStore());
    }

    Variable(@NotNull String name, @NotNull VariableStore owner) {
        this.name = Objects.requireNonNull(name);
        this.owner = Objects.requireNonNull(owner);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getValue() {
        return this.owner.getValue(this);
    }

    public void setValue(@Nullable String value) {
        this.owner.setValue(this, value);
    }

    public boolean isResetOnLaunch() {
        return this.owner.isResetOnLaunch(this);
    }

    public void setResetOnLaunch(boolean resetOnLaunch) {
        this.owner.setResetOnLaunch(this, resetOnLaunch);
    }

    public void toggleResetOnLaunch() {
        this.owner.toggleResetOnLaunch(this);
    }

    @NotNull
    public UserVariableSnapshot snapshot() {
        return this.owner.getSnapshot(this);
    }

    @NotNull
    public PropertyContainer serialize() {
        return this.owner.serialize(this);
    }

    @Nullable
    public static Variable deserialize(@NotNull PropertyContainer container) {
        return deserialize(container, VariableHandler.getStore());
    }

    @Nullable
    static Variable deserialize(@NotNull PropertyContainer container, @NotNull VariableStore owner) {
        String name = container.getValue("name");
        if (name == null) return null;
        Variable variable = new Variable(name, owner);
        variable.value = Objects.requireNonNullElse(container.getValue("value"), "");
        variable.resetOnLaunch = "true".equals(container.getValue("reset_on_launch"));
        return variable;
    }

    @NotNull
    static String encodeValue(@Nullable String value) {
        String nonNullValue = Objects.requireNonNullElse(value, "");
        return nonNullValue.replace("\n", NEWLINE_REPLACEMENT).replace("\r", NEWLINE_REPLACEMENT);
    }

    @NotNull
    VariableStore getOwner() {
        return this.owner;
    }

    @NotNull
    String getRawValueLocked() {
        return this.value;
    }

    @NotNull
    String getValueLocked() {
        return this.value.replace(NEWLINE_REPLACEMENT, "\n");
    }

    void setRawValueLocked(@NotNull String value) {
        this.value = Objects.requireNonNull(value);
    }

    boolean isResetOnLaunchLocked() {
        return this.resetOnLaunch;
    }

    void setResetOnLaunchLocked(boolean resetOnLaunch) {
        this.resetOnLaunch = resetOnLaunch;
    }

    @NotNull
    UserVariableSnapshot createSnapshotLocked() {
        return new UserVariableSnapshot(this.name, this.getValueLocked(), this.resetOnLaunch);
    }

    @NotNull
    PropertyContainer serializeLocked() {
        PropertyContainer container = new PropertyContainer("variable");
        container.putProperty("name", this.name);
        container.putProperty("value", this.value);
        container.putProperty("reset_on_launch", Boolean.toString(this.resetOnLaunch));
        return container;
    }

}
