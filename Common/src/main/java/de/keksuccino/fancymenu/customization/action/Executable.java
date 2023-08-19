package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public interface Executable {

    void execute();

    @NotNull
    String getIdentifier();

    @NotNull
    PropertyContainer serialize();

    default void serializeToExistingPropertyContainer(@NotNull PropertyContainer container) {
        PropertyContainer c = this.serialize();
        for (Map.Entry<String, String> m : c.getProperties().entrySet()) {
            container.putProperty(m.getKey(), m.getValue());
        }
    }

}
