package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * This is the base class for the bridge between the MCEF browser and FancyMenu to make it possible to execute FancyMenu's actions in the browser via JavaScript.
 */
public class ActionBridge {

    /**
     * This method deserializes browser action strings into actual {@link ActionInstance}s.<br><br>
     *
     * Browser action strings should have this format: {@code action_type:value}<br>
     * For actions without value, it's only the action type: {@code action_type}
     */
    @Nullable
    public static ActionInstance deserializeBrowserAction(@NotNull String actionString) {
        String actionType;
        String value = null;
        if (actionString.contains(":")) {
            var array = actionString.split(":", 2);
            actionType = array[0];
            value = array[1];
        } else {
            actionType = actionString;
        }
        String parsableKey = "[executable_action_instance:" + ScreenCustomization.generateUniqueIdentifier() + "][action_type:" + actionType + "]";
        PropertyContainer container = new PropertyContainer("dummy_action_holder");
        container.putProperty(parsableKey, (value == null) ? "" : value);
        List<ActionInstance> deserialized = ActionInstance.deserializeAll(container);
        if (!deserialized.isEmpty()) return deserialized.get(0);
        return null;
    }

}
