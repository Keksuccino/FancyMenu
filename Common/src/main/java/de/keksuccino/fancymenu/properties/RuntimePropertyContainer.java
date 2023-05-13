package de.keksuccino.fancymenu.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is used to temporarily store values at runtime.<br>
 * This container is NOT SERIALIZABLE!
 */
public class RuntimePropertyContainer {

    protected final Map<String, RuntimeProperty<?>> properties = new LinkedHashMap<>();

    public <T> RuntimePropertyContainer putProperty(String key, T value) {
        this.properties.put(key, new RuntimeProperty<T>(value));
        return this;
    }

    @Nullable
    public Boolean getBooleanProperty(@NotNull String key) {
        return this.getProperty(key, Boolean.class);
    }

    @Nullable
    public String getStringProperty(@NotNull String key) {
        return this.getProperty(key, String.class);
    }

    @Nullable
    public Integer getIntegerProperty(@NotNull String key) {
        return this.getProperty(key, Integer.class);
    }

    @SuppressWarnings("all")
    @Nullable
    public <T> T getProperty(@NotNull String key, @NotNull Class<? extends T> propertyType) {
        RuntimeProperty<?> p = this.properties.get(key);
        try {
            if (p != null) {
                RuntimeProperty<T> p2 = (RuntimeProperty<T>) p;
                return p2.value;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean hasProperty(String key) {
        return this.properties.containsKey(key);
    }

    public RuntimePropertyContainer removeProperty(@NotNull String key) {
        this.properties.remove(key);
        return this;
    }

    public static class RuntimeProperty<T> {

        public T value;

        public RuntimeProperty(T value) {
            this.value = value;
        }

    }

}
