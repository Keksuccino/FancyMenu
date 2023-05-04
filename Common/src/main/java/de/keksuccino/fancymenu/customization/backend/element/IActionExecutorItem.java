package de.keksuccino.fancymenu.customization.backend.element;

import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine;

import java.util.List;

public interface IActionExecutorItem {

    List<ButtonScriptEngine.ActionContainer> getActionList();

}
