package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class ElseExecutableBlock extends AbstractExecutableBlock {

    @Override
    public String getBlockType() {
        return "else";
    }

    public static ElseExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        ElseExecutableBlock b = new ElseExecutableBlock();
        b.identifier = identifier;
        return b;
    }

}
