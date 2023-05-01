package de.keksuccino.fancymenu.customization.backend.item.v2.items;

import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine;

import java.util.List;

public interface IActionExecutorItem {

    List<ButtonScriptEngine.ActionContainer> getActionList();

}
