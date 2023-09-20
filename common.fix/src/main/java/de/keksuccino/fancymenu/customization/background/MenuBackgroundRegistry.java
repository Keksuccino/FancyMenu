package de.keksuccino.fancymenu.customization.background;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MenuBackgroundRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, MenuBackgroundBuilder<?>> BACKGROUNDS = new LinkedHashMap<>();

    /**
     * Here you can register menu backgrounds which can then be used in layouts.<br>
     * {@link MenuBackgroundBuilder}s should get registered during mod-init.
     **/
    public static void register(@NotNull MenuBackgroundBuilder<?> builder) {
        if (BACKGROUNDS.containsKey(Objects.requireNonNull(builder.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] Menu background with identifier '" + builder.getIdentifier() + "' already registered! Overriding background!");
        }
        BACKGROUNDS.put(builder.getIdentifier(), builder);
    }

    @NotNull
    public static List<MenuBackgroundBuilder<?>> getBuilders() {
        return new ArrayList<>(BACKGROUNDS.values());
    }

    @Nullable
    public static MenuBackgroundBuilder<?> getBuilder(@NotNull String identifier) {
        return BACKGROUNDS.get(identifier);
    }

}
