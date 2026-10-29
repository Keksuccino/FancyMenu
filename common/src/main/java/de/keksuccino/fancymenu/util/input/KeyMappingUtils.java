package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyMappingUtils {

    /**
     * Returns all keybinds registered in Minecraft's options, including the ones added by mods.
     */
    @NotNull
    public static KeyMapping[] getKeyMappings() {
        return Minecraft.getInstance().options.keyMappings;
    }

    /**
     * Returns the registered keybind with the given identifier (like "key.jump") or NULL if there is none.<br>
     * Leading and trailing whitespace of the identifier gets ignored.
     */
    @Nullable
    public static KeyMapping findKeyMapping(@NotNull String identifier) {
        return findKeyMapping(getKeyMappings(), identifier);
    }

    /**
     * Returns the first keybind of the given array with the given identifier (like "key.jump") or NULL if there is none.<br>
     * Leading and trailing whitespace of the identifier gets ignored.
     */
    @Nullable
    public static KeyMapping findKeyMapping(@NotNull KeyMapping[] keyMappings, @NotNull String identifier) {
        String trimmedIdentifier = identifier.trim();
        for (KeyMapping keyMapping : keyMappings) {
            if (keyMapping.getName().equals(trimmedIdentifier)) return keyMapping;
        }
        return null;
    }

}
