package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ActionInstance implements Executable {

    //TODO add ELSE and ELSE-IF block and think about how to handle that sh!t

    //TODO update the action builder screen to support blocks

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public volatile Action action;
    @Nullable
    public volatile String value;
    @NotNull
    public String identifier = ScreenCustomization.generateUniqueIdentifier();

    public ActionInstance(@NotNull Action action, @Nullable String value) {
        this.action = Objects.requireNonNull(action);
        this.value = value;
    }

    @Override
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public void execute() {
        String v = this.value;
        try {
            if (v != null) {
                v = PlaceholderParser.replacePlaceholders(v);
            }
            if (this.action.hasValue()) {
                this.action.execute(v);
            } else {
                this.action.execute(null);
            }
        } catch (Exception ex) {
            LOGGER.error("################################");
            LOGGER.error("[FANCYMENU] An error occurred while trying to execute an action!");
            LOGGER.error("[FANCYMENU] Action: " + this.action.getIdentifier());
            LOGGER.error("[FANCYMENU] Value Raw: " + this.value);
            LOGGER.error("[FANCYMENU] Value: " + v);
            LOGGER.error("################################");
            ex.printStackTrace();
        }
    }

    @NotNull
    public ActionInstance copy(boolean unique) {
        ActionInstance i = new ActionInstance(this.action, this.value);
        if (!unique) i.identifier = this.identifier;
        return i;
    }

    @NotNull
    public PropertyContainer serialize() {
        PropertyContainer c = new PropertyContainer("executable_action_instance");
        String key = "[executable_action_instance:" + this.identifier + "][action_type:" + this.action.getIdentifier() + "]";
        String val = this.value;
        c.putProperty(key, (val != null) ? val : "");
        return c;
    }

    @NotNull
    public static List<ActionInstance> deserializeAll(@NotNull PropertyContainer serialized) {
        List<ActionInstance> instances = new ArrayList<>();
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            //Legacy button action handling
            if (m.getKey().equals("buttonaction")) {
                instances = deserializeLegacyButtonActions(serialized);
                break;
            }
            //Legacy ticker action handling
            if (m.getKey().startsWith("tickeraction_")) {
                instances = deserializeLegacyTickerActions(serialized);
                break;
            }
            //Deserialize normal action instance
            if (m.getKey().startsWith("[executable_action_instance:") && m.getKey().contains("]")) {
                String identifier = m.getKey().split("\\[executable_action_instance:", 2)[1].split("]", 2)[0];
                if (m.getKey().contains("[action_type:")) {
                    String actionType = m.getKey().split("\\[action_type:", 2)[1];
                    if (actionType.contains("]")) {
                        actionType = actionType.split("]", 2)[0];
                        Action action = ActionRegistry.getAction(actionType);
                        if (action != null) {
                            ActionInstance i = new ActionInstance(action, action.hasValue() ? m.getValue() : null);
                            i.identifier = identifier;
                            instances.add(i);
                        }
                    }
                }
            }
        }
        return instances;
    }

    @Legacy("This deserializes the old action format of button elements.")
    @NotNull
    protected static List<ActionInstance> deserializeLegacyButtonActions(@NotNull PropertyContainer serialized) {
        List<ActionInstance> instances = new ArrayList<>();
        String buttonAction = serialized.getValue("buttonaction");
        String actionValue = serialized.getValue("value");
        if (actionValue == null) {
            actionValue = "";
        }
        if (buttonAction != null) {
            if (buttonAction.contains("%btnaction_splitter_fm%")) {
                for (String s : StringUtils.splitLines(buttonAction, "%btnaction_splitter_fm%")) {
                    if (s.length() > 0) {
                        String actionIdentifier = s;
                        String value = null;
                        if (s.contains(";")) {
                            actionIdentifier = s.split(";", 2)[0];
                            value = s.split(";", 2)[1];
                        }
                        Action a = ActionRegistry.getAction(actionIdentifier);
                        if (a != null) {
                            instances.add(new ActionInstance(a, value));
                        }
                    }
                }
            } else {
                Action a = ActionRegistry.getAction(buttonAction);
                if (a != null) {
                    instances.add(new ActionInstance(a, actionValue));
                }
            }
        }
        return instances;
    }

    @Legacy("This deserializes the old action format of ticker elements.")
    @NotNull
    protected static List<ActionInstance> deserializeLegacyTickerActions(@NotNull PropertyContainer serialized) {
        List<ActionInstance> instances = new ArrayList<>();
        Map<Integer, ActionInstance> tempActions = new HashMap<>();
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().startsWith("tickeraction_")) {
                String index = m.getKey().split("_", 3)[1];
                String tickerAction = m.getKey().split("_", 3)[2];
                String actionValue = m.getValue();
                if (MathUtils.isInteger(index)) {
                    Action a = ActionRegistry.getAction(tickerAction);
                    if (a != null) {
                        tempActions.put(Integer.parseInt(index), new ActionInstance(a, actionValue));
                    }
                }
            }
        }
        List<Integer> indexes = new ArrayList<>(tempActions.keySet());
        Collections.sort(indexes);
        for (int i : indexes) {
            instances.add(tempActions.get(i));
        }
        return instances;
    }

}
