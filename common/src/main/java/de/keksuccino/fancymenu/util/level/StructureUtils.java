package de.keksuccino.fancymenu.util.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for structure-related operations.
 */
public class StructureUtils {

    /**
     * Checks if a BlockPos is within a specific structure.
     *
     * @param level The server level to check in
     * @param pos The position to check
     * @param structure The structure key to check for
     * @return true if the position is within the structure, false otherwise
     */
    public static boolean isInStructure(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull ResourceKey<Structure> structure) {
        if (!level.isLoaded(pos)) {
            return false; // Position not loaded, can't check
        }
        List<ResourceKey<Structure>> structures = getAllStructuresAt(level, pos);
        for (ResourceKey<Structure> key : structures) {
            if (key.toString().equals(structure.toString())) return true;
        }
        return false;
    }

    /**
     * Gets all structures at a specific BlockPos.
     *
     * @param level The server level to check in
     * @param pos The position to check
     * @return A list of ResourceKeys for all structures at this position
     */
    @NotNull
    public static List<ResourceKey<Structure>> getAllStructuresAt(@NotNull ServerLevel level, @NotNull BlockPos pos) {

        if (!level.isLoaded(pos)) {
            return List.of(); // Position not loaded, can't check
        }

        // Get all structures in the registry
        Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        List<ResourceKey<Structure>> keys = new ArrayList<>();
        level.structureManager().getAllStructuresAt(pos).forEach((structure, longs) -> {
            var structureKey = structureRegistry.getResourceKey(structure);
            structureKey.ifPresent(keys::add);
        });
        return keys;

    }

    /**
     * Gets a structure resource key from a string identifier.
     *
     * @param structureId The structure identifier (e.g., "minecraft:mansion")
     * @return The ResourceKey for the structure
     */
    @NotNull
    public static ResourceKey<Structure> getStructureKey(@NotNull String structureId) {
        ResourceLocation resourceLocation = ResourceLocation.parse(structureId);
        return getStructureKey(resourceLocation);
    }

    /**
     * Gets a structure resource key from a ResourceLocation.
     *
     * @param location The ResourceLocation for the structure
     * @return The ResourceKey for the structure
     */
    @NotNull
    public static ResourceKey<Structure> getStructureKey(@NotNull ResourceLocation location) {
        return ResourceKey.create(Registries.STRUCTURE, location);
    }

    /**
     * Gets all available structure resource keys from the registry.
     *
     * @param registryAccess The registry access to get structures from
     * @return A list of all structure resource keys
     */
    @NotNull
    public static List<ResourceKey<Structure>> getAllStructureKeys(@NotNull RegistryAccess registryAccess) {
        Registry<Structure> structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
        return new ArrayList<>(structureRegistry.registryKeySet());
    }

    /**
     * Tries to find a structure key by name, returning an Optional result.
     *
     * @param registryAccess The registry access to search in
     * @param structureName The name of the structure to find
     * @return An Optional containing the structure key if found, or empty if not found
     */
    @NotNull
    public static Optional<ResourceKey<Structure>> findStructureKey(@NotNull RegistryAccess registryAccess, @NotNull String structureName) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(structureName);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, resourceLocation);
            // Verify the key exists in the registry
            Registry<Structure> structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
            if (structureRegistry.containsKey(key)) {
                return Optional.of(key);
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @NotNull
    public static List<String> convertStructureKeysToStrings(@NotNull List<ResourceKey<Structure>> keys) {
        List<String> stringKeys = new ArrayList<>();
        keys.forEach(structureResourceKey -> stringKeys.add(structureResourceKey.location().toString()));
        return stringKeys;
    }

}