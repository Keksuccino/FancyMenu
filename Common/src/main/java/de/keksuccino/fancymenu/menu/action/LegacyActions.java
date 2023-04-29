
package de.keksuccino.fancymenu.menu.action;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.konkrete.localization.Locals;

import java.util.ArrayList;
import java.util.List;

public class LegacyActions {

    public static List<String> getLegacyIdentifiers() {
        List<String> actionIdentifiers = new ArrayList<>();
        actionIdentifiers.add("openlink");
        actionIdentifiers.add("sendmessage");
        actionIdentifiers.add("quitgame");
        actionIdentifiers.add("joinserver");
        actionIdentifiers.add("loadworld");
        actionIdentifiers.add("opencustomgui");
        actionIdentifiers.add("opengui");
        actionIdentifiers.add("openfile");
        actionIdentifiers.add("movefile");
        actionIdentifiers.add("copyfile");
        actionIdentifiers.add("deletefile");
        actionIdentifiers.add("renamefile");
        actionIdentifiers.add("downloadfile");
        actionIdentifiers.add("unpackzip");
        actionIdentifiers.add("reloadmenu");
        actionIdentifiers.add("runscript");
        actionIdentifiers.add("runcmd");
        actionIdentifiers.add("closegui");
        actionIdentifiers.add("copytoclipboard");
        actionIdentifiers.add("mimicbutton");
        actionIdentifiers.add("join_last_world");
        return actionIdentifiers;
    }

    public static List<ButtonActionContainer> buildLegacyActionContainers() {
        List<ButtonActionContainer> actions = new ArrayList<>();
        List<String> actionIdentifiers = getLegacyIdentifiers();
        for (String s : actionIdentifiers) {
            String actionDescKey = "helper.creator.custombutton.config.actiontype." + s + ".desc";
            String valueDescKey = "helper.creator.custombutton.config.actiontype." + s + ".desc.value";
            String valueExampleKey = "helper.creator.custombutton.config.actiontype." + s + ".desc.value.example";
            actions.add(buildContainer(s, actionDescKey, valueDescKey, valueExampleKey));
        }
        return actions;
    }

    private static ButtonActionContainer buildContainer(String identifier, String descLocalizationKey, String valueDescLocalizationKey, String valueExampleKey) {
        return new ButtonActionContainer(identifier) {
            @Override
            public String getAction() {
                return identifier;
            }
            @Override
            public boolean hasValue() {
                return !valueDescLocalizationKey.equals(Locals.localize(valueDescLocalizationKey));
            }
            @Override
            public void execute(String value) {
                // Do nothing, because action is executed in ButtonScriptEngine class
            }
            @Override
            public String getActionDescription() {
                if (descLocalizationKey != null) {
                    return Locals.localize(descLocalizationKey);
                }
                return null;
            }
            @Override
            public String getValueDescription() {
                if (valueDescLocalizationKey != null) {
                    return Locals.localize(valueDescLocalizationKey);
                }
                return null;
            }
            @Override
            public String getValueExample() {
                return Locals.localize(valueExampleKey);
            }
        };
    }

}
