package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.Executable;
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
        container.putProperty(key, value);
        return container;
    }

}
