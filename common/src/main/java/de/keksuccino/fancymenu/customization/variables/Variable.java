package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Variable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NEWLINE_REPLACEMENT = "+*||<FM_NEWLINE>||*+";

    @NotNull
    private final String name;
    /** The value of a text variable as it gets stored, with replaced line breaks. List variables keep it empty. */
    @NotNull
    private String value = "";
    /**
     * The entries of a list variable, or {@code null} for a text variable. Only the store changes them, under its lock, and every change
     * has to call {@link #listChangedLocked()} afterward, so the cached copy and text form get built again.
     */
    @Nullable
    private ArrayList<String> listEntries;
    /** Immutable copy of {@link #listEntries}, built on first use after a change, so reading a list every frame copies nothing. */
    @Nullable
    private List<String> listEntriesView;
    /** Text form of {@link #listEntries} (see {@link VariableLists#toText(List)}), built on first use after a change. */
    @Nullable
    private String listText;
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

    /**
     * Returns the text form of the variable. For lists, it is a JSON array of the entries, see {@link VariableLists#toText(List)}.
     */
    @NotNull
    public String getValue() {
        return this.owner.getValue(this);
    }

    /**
     * Sets the value from text. A list variable stays a list and reads the text as described in {@link VariableLists#fromAssignedText(String)}.
     */
    public void setValue(@Nullable String value) {
        this.owner.setValue(this, value);
    }

    @NotNull
    public VariableType getType() {
        return this.owner.getType(this);
    }

    public boolean isList() {
        return this.getType() == VariableType.LIST;
    }

    /**
     * @return An immutable copy of the entries of a list variable, or an empty list for a text variable.
     */
    @NotNull
    public List<String> getListEntries() {
        return this.owner.getListEntries(this);
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

    /**
     * Reads a variable of the database. Containers without a {@code type} are text variables, which is how all variables of older versions
     * look. A list whose value is no valid JSON array, or a type this version doesn't know, loads as a text variable with the stored value,
     * so nothing gets lost.
     */
    @Nullable
    static Variable deserialize(@NotNull PropertyContainer container, @NotNull VariableStore owner) {
        String name = container.getValue("name");
        if (name == null) return null;
        Variable variable = new Variable(name, owner);
        String storedValue = Objects.requireNonNullElse(container.getValue("value"), "");
        String storedType = container.getValue("type");
        VariableType type = (storedType != null) ? VariableType.getBySerializedName(storedType) : VariableType.TEXT;
        List<String> entries = (type == VariableType.LIST) ? VariableLists.parseText(storedValue) : null;
        if (entries != null) {
            variable.makeListLocked(entries);
        } else {
            if (type != VariableType.TEXT) LOGGER.warn("[FANCYMENU] Loaded variable '{}' as text, because its type '{}' or its value is not supported: {}", name, storedType, storedValue);
            variable.value = storedValue;
        }
        variable.resetOnLaunch = "true".equals(container.getValue("reset_on_launch"));
        return variable;
    }

    /**
     * Normalizes a variable name that got typed in or comes from placeholders, by removing the whitespace around it.<br>
     * It needs no variable store, so code that can't load {@link VariableHandler} (like unit tests, where it would need the game
     * directory) can use it too. {@link VariableHandler#normalizeVariableReferenceName(String)} does the same.
     */
    @NotNull
    public static String normalizeReferenceName(@NotNull String name) {
        return Objects.requireNonNull(name).strip();
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
    VariableType getTypeLocked() {
        return (this.listEntries != null) ? VariableType.LIST : VariableType.TEXT;
    }

    boolean isListLocked() {
        return this.listEntries != null;
    }

    /**
     * Returns the value as it gets stored: the encoded text of a text variable, or the text form of a list, which never contains line breaks.
     */
    @NotNull
    String getRawValueLocked() {
        return (this.listEntries != null) ? this.getListTextLocked() : this.value;
    }

    /** Returns the text form of the variable. */
    @NotNull
    String getValueLocked() {
        return (this.listEntries != null) ? this.getListTextLocked() : this.value.replace(NEWLINE_REPLACEMENT, "\n");
    }

    /** Makes the variable a text variable with the given encoded value (see {@link #encodeValue(String)}). */
    void setRawValueLocked(@NotNull String value) {
        this.value = Objects.requireNonNull(value);
        this.listEntries = null;
        this.listChangedLocked();
    }

    /** Empties the value and keeps the type, which is what Clear on Launch does. */
    void resetValueLocked() {
        if (this.listEntries != null) {
            this.listEntries.clear();
            this.listChangedLocked();
        } else {
            this.value = "";
        }
    }

    /** Makes the variable a list variable with a copy of the given entries. */
    void makeListLocked(@NotNull List<String> entries) {
        ArrayList<String> copy = new ArrayList<>(entries.size());
        for (String entry : entries) copy.add(Objects.requireNonNull(entry));
        this.listEntries = copy;
        this.value = "";
        this.listChangedLocked();
    }

    /**
     * Returns the entries the store edits in place. Only call it for list variables, and call {@link #listChangedLocked()} after changing them.
     */
    @NotNull
    ArrayList<String> getMutableListEntriesLocked() {
        return Objects.requireNonNull(this.listEntries, "Not a list variable");
    }

    /** Returns an immutable copy of the entries, or an empty list for a text variable. */
    @NotNull
    List<String> getListEntriesLocked() {
        if (this.listEntries == null) return List.of();
        List<String> view = this.listEntriesView;
        if (view == null) {
            view = List.copyOf(this.listEntries);
            this.listEntriesView = view;
        }
        return view;
    }

    @NotNull
    private String getListTextLocked() {
        String text = this.listText;
        if (text == null) {
            text = VariableLists.toText(this.getListEntriesLocked());
            this.listText = text;
        }
        return text;
    }

    /** Drops the cached copy and text form of the entries, which has to happen after every change of them. */
    void listChangedLocked() {
        this.listEntriesView = null;
        this.listText = null;
    }

    boolean isResetOnLaunchLocked() {
        return this.resetOnLaunch;
    }

    void setResetOnLaunchLocked(boolean resetOnLaunch) {
        this.resetOnLaunch = resetOnLaunch;
    }

    @NotNull
    UserVariableSnapshot createSnapshotLocked() {
        if (this.listEntries != null) return new UserVariableSnapshot(this.name, this.getListTextLocked(), this.resetOnLaunch, VariableType.LIST, this.getListEntriesLocked());
        return new UserVariableSnapshot(this.name, this.getValueLocked(), this.resetOnLaunch);
    }

    /** Makes the variable what the snapshot describes. */
    void applySnapshotLocked(@NotNull UserVariableSnapshot snapshot) {
        if (snapshot.isList()) {
            this.makeListLocked(snapshot.listEntries());
        } else {
            this.setRawValueLocked(encodeValue(snapshot.value()));
        }
        this.resetOnLaunch = snapshot.resetOnLaunch();
    }

    @NotNull
    PropertyContainer serializeLocked() {
        PropertyContainer container = new PropertyContainer("variable");
        container.putProperty("name", this.name);
        if (this.listEntries != null) container.putProperty("type", VariableType.LIST.getSerializedName());
        container.putProperty("value", this.getRawValueLocked());
        container.putProperty("reset_on_launch", Boolean.toString(this.resetOnLaunch));
        return container;
    }

}
