package de.keksuccino.fancymenu.customization.screenoverlay;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractOverlayBuilder<O extends AbstractOverlay> {

    protected static final String OVERLAY_PROPERTY_PREFIX = "screen_overlay";
    protected static final String OVERLAY_INSTANCE_IDENTIFIER_PROPERTY_PREFIX = "screen_overlay_instance";

    @NotNull
    protected final String identifier;

    public AbstractOverlayBuilder(@NotNull String identifier) {
        if (identifier.contains(":")) throw new RuntimeException("Using ':' in overlay identifiers is not allowed!");
        this.identifier = Objects.requireNonNull(identifier);
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public abstract O buildDefaultInstance();

    @NotNull
    protected abstract O deserializeFromProperties(@NotNull O instanceToWrite, @NotNull PropertyContainer deserializeFrom);

    @ApiStatus.Internal
    @NotNull
    public List<O> _deserializeAllFromProperties(@NotNull PropertyContainer deserializeFrom) {

        List<O> instances = new ArrayList<>();
        List<String> instanceIds = this.findAllOverlayInstancesInProperties(deserializeFrom);

        instanceIds.forEach(instanceIdentifier -> {

        });

        return instances;

    }

    protected abstract void serializeToProperties(@NotNull O instanceToRead, @NotNull PropertyContainer serializeTo);

    @ApiStatus.Internal
    public void _serializeToProperties(@NotNull O instanceToRead, @NotNull PropertyContainer serializeTo) {

        // Instance identifier
        serializeTo.putProperty(OVERLAY_INSTANCE_IDENTIFIER_PROPERTY_PREFIX + ":" + this.getIdentifier() + ":" + instanceToRead.getInstanceIdentifier(), "");

        this.serializeToProperties(instanceToRead, serializeTo);

    }

    @NotNull
    protected String getPropertyKeyPrefix(@NotNull O instance) {
        return OVERLAY_PROPERTY_PREFIX + ":" + this.getIdentifier() + ":" + instance.getInstanceIdentifier() + ":";
    }

    @Nullable
    protected String deserializeValue(@NotNull O instance, @NotNull PropertyContainer from, @NotNull String key) {
        var prefix = getPropertyKeyPrefix(instance);
        return from.getValue(prefix + key);
    }

    protected void serializeValue(@NotNull O instance, @NotNull PropertyContainer to, @NotNull String key, @Nullable String value) {
        if (value == null) return;
        var prefix = getPropertyKeyPrefix(instance);
        to.putProperty(prefix + key, value);
    }

    @NotNull
    protected List<String> findAllOverlayInstancesInProperties(@NotNull PropertyContainer container) {
        List<String> ids = new ArrayList<>();
        container.getProperties().keySet().forEach(key -> {
            if (key.startsWith(OVERLAY_INSTANCE_IDENTIFIER_PROPERTY_PREFIX + ":" + this.getIdentifier() + ":")) {
                ids.add(key.split(":", 3)[2]);
            }
        });
        return ids;
    }

    @Nullable
    protected Map<String, String> findAllPropertiesForInstance(@NotNull String instanceIdentifier, @NotNull PropertyContainer container) {

        Map<String, String> properties = new LinkedHashMap<>();



        return properties.isEmpty() ? null : properties;

    }

    protected record OverlayPropertyKey(@NotNull String builderIdentifier, @NotNull String instanceIdentifier, @NotNull String key) {

        @Nullable
        protected static OverlayPropertyKey parse(@NotNull String key) {
            if (!key.startsWith(OVERLAY_PROPERTY_PREFIX + ":")) return null;
            var a = key.split(":", 4);
            if (a.length < 4) return null;
            return new OverlayPropertyKey(a[1], a[2], a[3]);
        }

    }

}
