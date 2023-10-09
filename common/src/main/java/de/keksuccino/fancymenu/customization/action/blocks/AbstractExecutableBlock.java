package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractExecutableBlock implements Executable, ValuePlaceholderHolder {

    protected final List<Executable> executables = new ArrayList<>();
    /**
     * Placeholders do not get serialized, but get copied when calling copy().
     * They get added at runtime, mostly after creating a new {@link ActionInstance}.
     */
    @NotNull
    protected final Map<String, Supplier<String>> valuePlaceholders = new HashMap<>();
    @NotNull
    public String identifier = ScreenCustomization.generateUniqueIdentifier();

    @NotNull
    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    public abstract String getBlockType();

    public void execute() {
        for (Executable e : this.executables) {
            e.execute();
        }
    }

    /**
     * Value placeholders are for replacing parts of the {@link ActionInstance#value}.<br>
     * Value placeholders added to blocks get automatically added to  all child {@link Executable}s and appended blocks.<br><br>
     *
     * Placeholders use the $$ prefix, but don't include this prefix in the placeholder name.
     *
     * @param placeholder The placeholder base. Should be all lowercase with no special chars or spaces. Use only [a-z], [0-9], [_], [-].
     * @param replaceWithSupplier The supplier that returns the actual value this placeholder should get replaced with.
     */
    public void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        if (!CharacterFilter.buildResourceNameCharacterFilter().isAllowedText(placeholder)) {
            throw new RuntimeException("Illegal characters used in placeholder name! Use only [a-z], [0-9], [_], [-]!");
        }
        this.valuePlaceholders.put(placeholder, replaceWithSupplier);
        for (Executable e : this.executables) {
            if (e instanceof ValuePlaceholderHolder h) {
                h.addValuePlaceholder(placeholder, replaceWithSupplier);
            }
        }
        if (this.getAppendedBlock() != null) {
            this.getAppendedBlock().addValuePlaceholder(placeholder, replaceWithSupplier);
        }
    }

    @NotNull
    @Override
    public Map<String, Supplier<String>> getValuePlaceholders() {
        return this.valuePlaceholders;
    }

    public List<Executable> getExecutables() {
        return this.executables;
    }

    public AbstractExecutableBlock addExecutable(Executable executable) {
        this.executables.add(executable);
        if (executable instanceof ValuePlaceholderHolder h) {
            this.valuePlaceholders.forEach(h::addValuePlaceholder);
        }
        return this;
    }

    public AbstractExecutableBlock removeExecutable(Executable executable) {
        this.executables.remove(executable);
        return this;
    }

    public AbstractExecutableBlock clearExecutables() {
        this.executables.clear();
        return this;
    }

    @Nullable
    public AbstractExecutableBlock getAppendedBlock() {
        return null;
    }

    public void setAppendedBlock(@Nullable AbstractExecutableBlock appended) {
        if (appended != null) {
            this.valuePlaceholders.forEach(appended::addValuePlaceholder);
        }
    }

    @Override
    @NotNull
    public PropertyContainer serialize() {
        PropertyContainer container = new PropertyContainer("executable_block");
        String key = "[executable_block:" + this.identifier + "][type:" + this.getBlockType() + "]";
        String value = "[executables:";
        for (Executable e : this.executables) {
            value += e.getIdentifier() + ";";
            e.serializeToExistingPropertyContainer(container);
        }
        value += "]";
        if (this.getAppendedBlock() != null) {
            value += "[appended:" + this.getAppendedBlock().getIdentifier() + "]";
        }
        container.putProperty(key, value);
        if (this.getAppendedBlock() != null) {
            this.getAppendedBlock().serializeToExistingPropertyContainer(container);
        }
        return container;
    }

}
