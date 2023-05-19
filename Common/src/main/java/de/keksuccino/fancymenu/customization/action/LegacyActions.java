
package de.keksuccino.fancymenu.customization.action;

import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

public class LegacyActions {

    //TODO Legacy actions zu neuem system porten

    public static List<String> getLegacyIdentifiers() {
        List<String> actionIdentifiers = new ArrayList<>();
        actionIdentifiers.add("openlink");
        actionIdentifiers.add("sendmessage");
        actionIdentifiers.add("quitgame");
        actionIdentifiers.add("joinserver");
        actionIdentifiers.add("loadworld");
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

    public static List<Action> buildLegacyActionContainers() {
        List<Action> actions = new ArrayList<>();
        List<String> actionIdentifiers = getLegacyIdentifiers();
        for (String s : actionIdentifiers) {
            String actionDescKey = "fancymenu.editor.custombutton.config.actiontype." + s + ".desc";
            String valueDescKey = "fancymenu.editor.custombutton.config.actiontype." + s + ".desc.value";
            String valueExampleKey = "fancymenu.editor.custombutton.config.actiontype." + s + ".desc.value.example";
            actions.add(buildContainer(s, actionDescKey, valueDescKey, valueExampleKey));
        }
        return actions;
    }

    private static Action buildContainer(String identifier, String descLocalizationKey, String valueDescLocalizationKey, String valueExampleKey) {
        return new Action(identifier) {
            @Override
            public boolean hasValue() {
                return !valueDescLocalizationKey.equals(I18n.get(valueDescLocalizationKey));
            }
            @Override
            public void execute(String value) {
                // Do nothing, because action is executed in ButtonScriptEngine class
            }
            @Override
            public String getActionDescription() {
                if (descLocalizationKey != null) {
                    return I18n.get(descLocalizationKey);
                }
                return null;
            }
            @Override
            public String getValueDescription() {
                if (valueDescLocalizationKey != null) {
                    return I18n.get(valueDescLocalizationKey);
                }
                return null;
            }
            @Override
            public String getValueExample() {
                return I18n.get(valueExampleKey);
            }
        };
    }

}
