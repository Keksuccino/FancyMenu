package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public abstract class AbstractDecorationOverlayBuilder<O extends AbstractDecorationOverlay> extends SerializationUtils {

    protected static final CharacterFilter IDENTIFIER_VALIDATOR = CharacterFilter.buildResourceNameFilter();

    protected static final String OVERLAY_PROPERTY_CONTAINER_TYPE = "decoration_overlay";

    private static final String OVERLAY_TYPE_KEY = "overlay_type";
    private static final String INSTANCE_IDENTIFIER_KEY = "instance_identifier";
    private static final String SHOW_OVERLAY_KEY = "show_decoration_overlay";

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
                instance.showOverlay = deserializeBoolean(instance.showOverlay, serialized.getValue(SHOW_OVERLAY_KEY));

                this.deserialize(instance, serialized);

                instances.add(instance);

            }
        });

        return instances;

    }

    protected abstract void serialize(@NotNull O instanceToSerialize, @NotNull PropertyContainer serializeTo);

    @ApiStatus.Internal
    @NotNull
    public PropertyContainer _serialize(@NotNull AbstractDecorationOverlay instanceToSerialize) {

        PropertyContainer serializeTo = new PropertyContainer(OVERLAY_PROPERTY_CONTAINER_TYPE);
        serializeTo.setInvulnerableProperties(true);

        serializeTo.putProperty(OVERLAY_TYPE_KEY, this.getIdentifier());
        serializeTo.putProperty(INSTANCE_IDENTIFIER_KEY, instanceToSerialize.getInstanceIdentifier());
        serializeTo.putProperty(SHOW_OVERLAY_KEY, instanceToSerialize.showOverlay);

        this.serialize((O) instanceToSerialize, serializeTo);

        return serializeTo;

    }

    protected abstract void buildConfigurationMenu(@NotNull O instance, @NotNull ContextMenu menu);

    @ApiStatus.Internal
    @NotNull
    public ContextMenu _buildConfigurationMenu(@NotNull AbstractDecorationOverlay instance) {

        ContextMenu menu = new ContextMenu();

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "show_overlay",
                () -> instance.showOverlay,
                aBoolean -> instance.showOverlay = aBoolean,
                "fancymenu.decoration_overlay.show_overlay");

        menu.addSeparatorEntry("separator_after_show_overlay_toggle");

        this.buildConfigurationMenu((O) instance, menu);

        return menu;

    }

    @NotNull
    public abstract Component getDisplayName();

    @Nullable
    public abstract Component getDescription();

}
