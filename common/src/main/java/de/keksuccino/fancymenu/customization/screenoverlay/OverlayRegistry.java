package de.keksuccino.fancymenu.customization.screenoverlay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class OverlayRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, AbstractOverlayBuilder<?>> BUILDERS = new LinkedHashMap<>();

    public static void register(@NotNull AbstractOverlayBuilder<?> builder) {
        Objects.requireNonNull(builder);
        if (getByIdentifier(builder.getIdentifier()) != null) {
            LOGGER.error("[FANCYMENU] Failed to register AbstractOverlayBuilder! Builder with same identifier already registered!" + builder.getIdentifier(), new IllegalStateException("Identifier already registered: " + builder.getIdentifier()));
            return;
        }
        BUILDERS.put(builder.getIdentifier(), builder);
    }

    @Nullable
    public static AbstractOverlayBuilder<?> getByIdentifier(@NotNull String identifier) {
        return BUILDERS.get(identifier);
    }

    @NotNull
    public static List<AbstractOverlayBuilder<?>> getAll() {
        return new ArrayList<>(BUILDERS.values());
    }

}
