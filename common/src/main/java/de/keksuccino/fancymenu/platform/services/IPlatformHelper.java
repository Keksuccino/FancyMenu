package de.keksuccino.fancymenu.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    String getPlatformDisplayName();

    String getLoaderVersion();

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

    @Nullable
    ResourceLocation getItemKey(@NotNull Item item);

    @Nullable
    ResourceLocation getEffectKey(@NotNull MobEffect effect);

    @Nullable
    ResourceLocation getEntityKey(@NotNull EntityType<?> type);

    @NotNull
    List<UniversalModContainer> getLoadedMods();

    @Nullable
    default UniversalModContainer getLoadedMod(@NotNull String id) {
        for (UniversalModContainer mod : this.getLoadedMods()) {
            if (id.equals(mod.id())) return mod;
        }
        return null;
    }

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

}