package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.input.TextValidators;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public enum ResourceSourceType {

    LOCATION,
    LOCAL,
    WEB;

    /**
     * Tries to find the {@link ResourceSourceType} of a resource's source.
     *
     * @param resourceSource Can be a URL to a web source, a path to a local source or a resource location (namespace:path).
     */
    @NotNull
    public static ResourceSourceType getSourceTypeOf(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource);
        if (TextValidators.BASIC_URL_TEXT_VALIDATOR.get(resourceSource)) return WEB;
        if (resourceSource.contains(":")) {
            if (ResourceLocation.tryParse(resourceSource) != null) return LOCATION;
        }
        return LOCAL;
    }

}
