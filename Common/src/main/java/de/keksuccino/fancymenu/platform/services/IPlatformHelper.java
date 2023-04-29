package de.keksuccino.fancymenu.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.List;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /** Get the version of a mod. **/
    String getModVersion(String modId);

    /** A list with mod IDs of all loaded mods. **/
    List<String> getLoadedModIds();

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /** If the mod is loaded client-side. **/
    boolean isOnClient();

    /** Get the key of a {@link KeyMapping}. **/
    InputConstants.Key getKeyMappingKey(KeyMapping keyMapping);

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

}