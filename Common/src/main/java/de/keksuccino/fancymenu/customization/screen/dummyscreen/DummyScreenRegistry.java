package de.keksuccino.fancymenu.customization.screen.dummyscreen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class DummyScreenRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, DummyScreenBuilder> BUILDERS = new LinkedHashMap<>();

    public static void register(@NotNull DummyScreenBuilder builder) {
        Objects.requireNonNull(builder);
        if (BUILDERS.containsKey(builder.screenIdentifier)) {
            LOGGER.warn("[FANCYMENU] DummyScreenBuilder for screen identifier '" + builder.screenIdentifier + "' already exists! Replacing builder..");
        }
        BUILDERS.put(builder.screenIdentifier, builder);
    }

    @Nullable
    public static DummyScreenBuilder getBuilderFor(@NotNull String screenIdentifier) {
        return BUILDERS.get(screenIdentifier);
    }

    @NotNull
    public static List<DummyScreenBuilder> getBuilders() {
        return new ArrayList<>(BUILDERS.values());
    }

}
