package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractExecutableBlock implements Executable {

    @Nullable
    public AbstractExecutableBlock parent;
    protected final List<Executable> executables = new ArrayList<>();
    @NotNull
    public String identifier = ScreenCustomization.generateUniqueIdentifier();

    public void execute() {
        for (Executable e : this.executables) {
            e.execute();
        }
    }

    public List<Executable> getExecutables() {
        return this.executables;
    }

    public AbstractExecutableBlock addExecutable(Executable executable) {
        this.executables.add(executable);
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

    //TODO Make button elements and ticker elements use an ExecutableBlock instance instead of lists with action instances

    @NotNull
    public PropertyContainer serialize() {

    }

    public void serializeToExistingPropertiesContainer(@NotNull PropertyContainer container) {

    }

    //TODO remove the following two methods and only add them to all sub classes of AbstractExecutableBlock, to be able to deserialize them
    @Nullable
    public static AbstractExecutableBlock deserializeWithIdentifier(@NotNull String identifier, @NotNull PropertyContainer serialized) {

    }

    @NotNull
    public static List<AbstractExecutableBlock> deserializeAll(@NotNull PropertyContainer serialized) {

    }

}
