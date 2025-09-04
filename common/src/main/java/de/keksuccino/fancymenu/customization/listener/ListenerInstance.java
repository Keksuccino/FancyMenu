package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListenerInstance {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @NotNull
    public final AbstractListener parent;
    @NotNull
    public GenericExecutableBlock actionScript = new GenericExecutableBlock();

    public ListenerInstance(@NotNull AbstractListener parent) {
        this.parent = parent;
    }

    /**
     * This method is safe for multi-calling, so even if the instance is already registered, nothing will break.
     */
    public void registerSelfToParent() {
        this.parent.registerInstance(this);
    }

    public PropertyContainer serialize() {

        PropertyContainer serialized = new PropertyContainer("listener_instance");

        serialized.putProperty("listener_instance_identifier", this.instanceIdentifier);
        serialized.putProperty("listener_provider_identifier", this.parent.getIdentifier());
        serialized.putProperty("listener_instance_action_script_identifier", this.actionScript.getIdentifier());

        this.actionScript.serializeToExistingPropertyContainer(serialized);

        return serialized;

    }

    @Nullable
    public static ListenerInstance deserialize(@NotNull PropertyContainer serialized) {

        if (!"listener_instance".equals(serialized.getType())) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Provided PropertyContainer does not hold a valid serialized listener instance! Wrong type: " + serialized.getType());
            return null;
        }

        String providerIdentifier = serialized.getValue("listener_provider_identifier");
        if (providerIdentifier == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Provider identifier was NULL!", new NullPointerException("Provider identifier was NULL"));
            return null;
        }
        AbstractListener provider = ListenerRegistry.getListener(providerIdentifier);
        if (provider == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Provider was NULL: " + providerIdentifier, new NullPointerException("Provider was NULL"));
            return null;
        }

        ListenerInstance instance = provider.createFreshInstance();

        instance.instanceIdentifier = Objects.requireNonNullElse(serialized.getValue("listener_instance_identifier"), ScreenCustomization.generateUniqueIdentifier());

        String actionScriptIdentifier = serialized.getValue("listener_instance_action_script_identifier");
        if (actionScriptIdentifier == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Action script identifier was NULL for instance with identifier: " + instance.instanceIdentifier, new NullPointerException("Action script identifier was NULL"));
            return null;
        }

        AbstractExecutableBlock executableBlock = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, actionScriptIdentifier);
        if (executableBlock == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Action script failed to get deserialized and was NULL for instance with identifier: " + instance.instanceIdentifier, new NullPointerException("Action script was NULL"));
            return null;
        } else if (executableBlock instanceof GenericExecutableBlock g) {
            instance.actionScript = g;
            LOGGER.info("########################## Block deserialized !!!!!!!!!!!!!!!!!!!!! " + g.getIdentifier() + " | " + g.getExecutables());
        } else {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instance! Action script is not a GenericExecutableBlock for instance with identifier: " + instance.instanceIdentifier, new ClassCastException("Block is not a GenericExecutableBlock"));
            return null;
        }

        return instance;

    }

    @NotNull
    public static List<ListenerInstance> deserializeAllFromSet(@NotNull PropertyContainerSet propertyContainerSet) {
        List<ListenerInstance> instances = new ArrayList<>();
        propertyContainerSet.getContainersOfType("listener_instance").forEach(propertyContainer -> {
            ListenerInstance instance = deserialize(propertyContainer);
            if (instance != null) {
                instances.add(instance);
            }
        });
        return instances;
    }

}
