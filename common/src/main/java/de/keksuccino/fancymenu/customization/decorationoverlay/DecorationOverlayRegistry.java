package de.keksuccino.fancymenu.customization.decorationoverlay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class DecorationOverlayRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, AbstractDecorationOverlayBuilder<?>> BUILDERS = new LinkedHashMap<>();

    public static void register(@NotNull AbstractDecorationOverlayBuilder<?> builder) {
        Objects.requireNonNull(builder);
        if (getByIdentifier(builder.getIdentifier()) != null) {
            LOGGER.error("[FANCYMENU] Failed to register AbstractOverlayBuilder! Builder with same identifier already registered!" + builder.getIdentifier(), new IllegalStateException("Identifier already registered: " + builder.getIdentifier()));
            return;
        }
        BUILDERS.put(builder.getIdentifier(), builder);
    }

    @Nullable
    public static AbstractDecorationOverlayBuilder<?> getByIdentifier(@NotNull String identifier) {
        return BUILDERS.get(identifier);
    }

    @NotNull
    public static List<AbstractDecorationOverlayBuilder<?>> getAll() {
        return new ArrayList<>(BUILDERS.values());
    }

}
