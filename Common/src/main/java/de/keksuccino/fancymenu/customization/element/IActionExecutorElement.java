package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.action.ExecutableAction;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface IActionExecutorElement {

    @NotNull
    List<ExecutableAction> getActionList();

}
