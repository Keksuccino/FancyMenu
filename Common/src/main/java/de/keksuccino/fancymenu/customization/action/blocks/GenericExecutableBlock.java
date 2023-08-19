package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class GenericExecutableBlock extends AbstractExecutableBlock {

    @Override
    public String getBlockType() {
        return "generic";
    }

    public static GenericExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        GenericExecutableBlock b = new GenericExecutableBlock();
        b.identifier = identifier;
        return b;
    }

}
