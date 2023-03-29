//TODO übernehmenn
package de.keksuccino.fancymenu.menu.button.buttonactions;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.konkrete.localization.Locals;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyButtonActions {

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

    protected static List<ButtonActionContainer> buildLegacyActionContainers() {
        List<ButtonActionContainer> actions = new ArrayList<>();
        List<String> actionIdentifiers = getLegacyIdentifiers();
        for (String s : actionIdentifiers) {
            String actionDescKey = "helper.creator.custombutton.config.actiontype." + s + ".desc";
            String valueDescKey = "helper.creator.custombutton.config.actiontype." + s + ".desc.value";
            String valueExample = Locals.localizeTo("helper.creator.custombutton.config.actiontype." + s + ".desc.value.example", "en_us");
            actions.add(buildContainer(s, actionDescKey, valueDescKey, valueExample));
        }
        return actions;
    }

    private static ButtonActionContainer buildContainer(String identifier, String descLocalizationKey, String valueDescLocalizationKey, String valueExample) {
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
//                ButtonScriptEngine.runButtonAction(identifier, value);
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
            @Nullable
            @Override
            public String getValueExample() {
                return valueExample;
            }
        };
    }

}
