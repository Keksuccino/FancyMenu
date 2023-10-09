package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class GenericExecutableBlock extends AbstractExecutableBlock {

    @Override
    public String getBlockType() {
        return "generic";
    }

    @Override
    public @NotNull GenericExecutableBlock copy(boolean unique) {
        GenericExecutableBlock b = new GenericExecutableBlock();
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        return b;
    }

    public static GenericExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        GenericExecutableBlock b = new GenericExecutableBlock();
        b.identifier = identifier;
        return b;
    }

}
