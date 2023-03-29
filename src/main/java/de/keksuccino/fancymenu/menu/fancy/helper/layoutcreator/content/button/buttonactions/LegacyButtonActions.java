package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions;

import de.keksuccino.konkrete.localization.Locals;

import java.util.ArrayList;
import java.util.List;

public class LegacyButtonActions {

    public static List<ButtonActionScreen.ButtonAction> getLegacyActions(ButtonActionScreen screen) {
        List<ButtonActionScreen.ButtonAction> actions = new ArrayList<>();
        List<String> actionNames = new ArrayList<>();
        actionNames.add("openlink");
        actionNames.add("sendmessage");
        actionNames.add("quitgame");
        actionNames.add("joinserver");
        actionNames.add("loadworld");
        actionNames.add("opencustomgui");
        actionNames.add("opengui");
        actionNames.add("openfile");
        actionNames.add("movefile");
        actionNames.add("copyfile");
        actionNames.add("deletefile");
        actionNames.add("renamefile");
        actionNames.add("downloadfile");
        actionNames.add("unpackzip");
        actionNames.add("reloadmenu");
        actionNames.add("runscript");
        actionNames.add("runcmd");
        actionNames.add("closegui");
        actionNames.add("copytoclipboard");
        actionNames.add("mimicbutton");
        actionNames.add("join_last_world");
        for (String s : actionNames) {
            String actionDesc = Locals.localize("helper.creator.custombutton.config.actiontype." + s + ".desc");
            String valueDescKey = "helper.creator.custombutton.config.actiontype." + s + ".desc.value";
            String valueDesc = Locals.localize(valueDescKey);
            boolean hasValue = (valueDesc != valueDescKey);
            String valueExample = Locals.localize("helper.creator.custombutton.config.actiontype." + s + ".desc.value.example");
            if (!hasValue) {
                valueDesc = null;
                valueExample = null;
            }
            actions.add(new ButtonActionScreen.ButtonAction(screen, s, actionDesc, hasValue, valueDesc, valueExample, null));
        }
        return actions;
    }

}
