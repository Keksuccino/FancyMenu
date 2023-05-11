package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.action.ActionExecutor;

import java.util.List;

public interface IActionExecutorElement {

    List<ActionExecutor.ActionContainer> getActionList();

}
