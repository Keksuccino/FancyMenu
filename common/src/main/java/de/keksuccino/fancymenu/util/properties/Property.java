package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings("unused")
public class Property<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected final String key;
    @Nullable
    protected T defaultValue;
    @Nullable
    protected T currentValue;
    @Nullable
    protected ConsumingSupplier<String, T> deserializationCodec;
    @Nullable
    protected ConsumingSupplier<T, String> serializationCodec = Object::toString;

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue) {
        Property<String> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = consumes -> consumes;
        return p;
    }

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue) {
        Property<String> p = new Property<>(key, defaultValue);
        p.deserializationCodec = consumes -> consumes;
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, int currentValue) {
        Property<Integer> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Integer::valueOf;
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue) {
        Property<Integer> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Integer::valueOf;
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, double currentValue) {
        Property<Double> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Double::valueOf;
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue) {
        Property<Double> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Double::valueOf;
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, long currentValue) {
        Property<Long> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Long::valueOf;
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue) {
        Property<Long> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Long::valueOf;
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, float currentValue) {
        Property<Float> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Float::valueOf;
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue) {
        Property<Float> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Float::valueOf;
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue) {
        Property<Boolean> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Boolean::valueOf;
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue) {
        Property<Boolean> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Boolean::valueOf;
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @Nullable ResourceSource currentValue) {
        Property<ResourceSource> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = ResourceSource::getSerializationSource;
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue) {
        Property<ResourceSource> p = new Property<>(key, defaultValue);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = ResourceSource::getSerializationSource;
        return p;
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @Nullable ResourceSupplier<R> currentValue) {
        Property<ResourceSupplier<R>> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = consumes -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.image(consumes);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.audio(consumes);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.video(consumes);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.text(consumes);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to deserialize ResourceSupplier property!");
        };
        p.serializationCodec = ResourceSupplier::getSourceWithPrefix;
        return p;
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue) {
        return resourceSupplierProperty(resourceType, key, defaultValue, defaultValue);
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, @Nullable DrawableColor currentValue) {
        Property<DrawableColor> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = DrawableColor::getHex;
        return p;
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue) {
        Property<DrawableColor> p = new Property<>(key, defaultValue);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = DrawableColor::getHex;
        return p;
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue) {
        this.key = Objects.requireNonNull(key);
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
    }

    protected Property(@NotNull String key, @Nullable T defaultValue) {
        this.key = Objects.requireNonNull(key);
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
    }

    public @NotNull String getKey() {
        return key;
    }

    public @Nullable T getDefault() {
        return defaultValue;
    }

    public Property<T> setDefault(@Nullable T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public @Nullable T get() {
        return currentValue;
    }

    /**
     * First tries to return the current value if it is not null, else will try to return the default value, and will throw an error if both are null.
     */
    public T tryGetNonNull() {
        if (this.currentValue != null) return this.currentValue;
        return Objects.requireNonNull(this.defaultValue);
    }

    /**
     * First tries to return the current value if it is not null, then tries to return the default value. If both are null, it will return {@code elseValue}.
     */
    public T tryGetNonNullElse(@NotNull T elseValue) {
        if (this.currentValue != null) return this.currentValue;
        if (this.defaultValue != null) return this.defaultValue;
        return Objects.requireNonNull(elseValue);
    }

    public Property<T> set(@Nullable T currentValue) {
        this.currentValue = currentValue;
        return this;
    }

    public @Nullable ConsumingSupplier<String, T> getDeserializationCodec() {
        return deserializationCodec;
    }

    public Property<T> setDeserializationCodec(@Nullable ConsumingSupplier<String, T> deserializationCodec) {
        this.deserializationCodec = deserializationCodec;
        return this;
    }

    public @Nullable ConsumingSupplier<T, String> getSerializationCodec() {
        return serializationCodec;
    }

    public Property<T> setSerializationCodec(@Nullable ConsumingSupplier<T, String> serializationCodec) {
        this.serializationCodec = serializationCodec;
        return this;
    }

    public Property<T> deserialize(@NotNull PropertyContainer properties) {
        if (this.deserializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), new NullPointerException("No deserialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            String serialized = properties.getValue(this.getKey());
            this.currentValue = (serialized != null) ? this.deserializationCodec.get(serialized) : this.defaultValue;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
        }
        return this;
    }

    public Property<T> serialize(@NotNull PropertyContainer properties) {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            properties.putProperty(this.getKey(), this.serializationCodec.get(this.currentValue));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return this;
    }

    @Override
    @Nullable
    public String toString() {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return null;
        }
        try {
            return this.serializationCodec.get(this.currentValue);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return null;
    }

}
