package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.action.ActionExecutor;

import java.util.List;

public interface IActionExecutorItem {

    List<ActionExecutor.ActionContainer> getActionList();

}
