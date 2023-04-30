package de.keksuccino.fancymenu.customization.item.v2.items;

import de.keksuccino.fancymenu.customization.button.ButtonScriptEngine;

import java.util.List;

public interface IActionExecutorItem {

    List<ButtonScriptEngine.ActionContainer> getActionList();

}
