package de.keksuccino.fancymenu.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PropertyHolder {

    @NotNull
    Map<String, Property<?>> getPropertyMap();

    @NotNull
    default List<Property<?>> getProperties() {
        return new ArrayList<>(this.getPropertyMap().values());
    }

    @Nullable
    default Property<?> getProperty(@NotNull String key) {
        return this.getPropertyMap().get(key);
    }

    default <P extends Property<?>> P putProperty(@NotNull P property) {
        this.getPropertyMap().put(property.getKey(), property);
        return property;
    }

    default void removeProperty(@NotNull String key) {
        this.getPropertyMap().remove(key);
    }

}
