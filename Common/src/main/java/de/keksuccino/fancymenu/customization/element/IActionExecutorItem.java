package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.button.ButtonScriptEngine;

import java.util.List;

public interface IActionExecutorItem {

    List<ButtonScriptEngine.ActionContainer> getActionList();

}
