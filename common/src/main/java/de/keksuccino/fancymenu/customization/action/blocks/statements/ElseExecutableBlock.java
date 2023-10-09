package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class ElseExecutableBlock extends AbstractExecutableBlock {

    @Override
    public String getBlockType() {
        return "else";
    }

    @Override
    public @NotNull ElseExecutableBlock copy(boolean unique) {
        ElseExecutableBlock b = new ElseExecutableBlock();
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        return b;
    }

    public static ElseExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        ElseExecutableBlock b = new ElseExecutableBlock();
        b.identifier = identifier;
        return b;
    }

}
