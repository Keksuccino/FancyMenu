package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import org.jetbrains.annotations.NotNull;

public interface ExecutableElement {

    @NotNull
    GenericExecutableBlock getExecutableBlock();

}
