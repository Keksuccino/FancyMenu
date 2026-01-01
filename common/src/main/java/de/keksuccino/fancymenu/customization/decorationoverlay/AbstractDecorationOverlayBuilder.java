package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public abstract class AbstractDecorationOverlayBuilder<O extends AbstractDecorationOverlay<?>> implements SerializationHelper {

    protected static final CharacterFilter IDENTIFIER_VALIDATOR = CharacterFilter.buildResourceNameFilter();

    protected static final String OVERLAY_PROPERTY_CONTAINER_TYPE = "decoration_overlay";

    private static final String OVERLAY_TYPE_KEY = "overlay_type";
    private static final String INSTANCE_IDENTIFIER_KEY = "instance_identifier";

    @NotNull
    protected final String identifier;

    public AbstractDecorationOverlayBuilder(@NotNull String identifier) {
        if (!IDENTIFIER_VALIDATOR.isAllowedText(identifier)) throw new RuntimeException("You can only use [a-z], [0-9], [_], [-] in overlay identifiers!");
        if (identifier.contains(":") || identifier.contains(".")) throw new RuntimeException("Using ':' and '.' in overlay identifiers is not allowed!");
        this.identifier = Objects.requireNonNull(identifier);
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public abstract O buildDefaultInstance();

    protected abstract void deserialize(@NotNull O instanceToWrite, @NotNull PropertyContainer deserializeFrom);

    @ApiStatus.Internal
    @NotNull
    public List<O> _deserializeAll(@NotNull PropertyContainerSet deserializeFrom) {

        List<O> instances = new ArrayList<>();
        List<PropertyContainer> serializedInstances = deserializeFrom.getContainersOfType(OVERLAY_PROPERTY_CONTAINER_TYPE);

        serializedInstances.forEach(serialized -> {
            String overlayType = serialized.getValue(OVERLAY_TYPE_KEY);
            if (this.getIdentifier().equals(overlayType)) {

                var instance = this.buildDefaultInstance();

                instance.setInstanceIdentifier(Objects.requireNonNullElse(serialized.getValue(INSTANCE_IDENTIFIER_KEY), ScreenCustomization.generateUniqueIdentifier()));

                instance.getPropertyMap().values().forEach(property -> {
                    property.deserialize(serialized);
                });

                this.deserialize(instance, serialized);

                instances.add(instance);

            }
        });

        return instances;

    }

    protected abstract void serialize(@NotNull O instanceToSerialize, @NotNull PropertyContainer serializeTo);

    @ApiStatus.Internal
    @NotNull
    public PropertyContainer _serialize(@NotNull AbstractDecorationOverlay<?> instanceToSerialize) {

        PropertyContainer serializeTo = new PropertyContainer(OVERLAY_PROPERTY_CONTAINER_TYPE);
        serializeTo.setInvulnerableProperties(true);

        serializeTo.putProperty(OVERLAY_TYPE_KEY, this.getIdentifier());
        serializeTo.putProperty(INSTANCE_IDENTIFIER_KEY, instanceToSerialize.getInstanceIdentifier());

        instanceToSerialize.getPropertyMap().values().forEach(property -> {
            property.serialize(serializeTo);
        });

        this.serialize((O) instanceToSerialize, serializeTo);

        return serializeTo;

    }

    @NotNull
    public abstract Component getDisplayName();

    @Nullable
    public abstract Component getDescription();

}
