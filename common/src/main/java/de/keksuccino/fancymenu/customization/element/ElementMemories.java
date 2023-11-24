package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElementMemories {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, RuntimePropertyContainer> ELEMENT_MEMORIES = new HashMap<>();

    public static void init() {
        EventHandler.INSTANCE.registerListenersOf(new ElementMemories());
    }

    @EventListener
    public void onReloadMod(ModReloadEvent e) {
        LOGGER.info("[FANCYMENU] Clearing element memories..");
        clearMemories();
    }

    @NotNull
    public static RuntimePropertyContainer getMemory(@NotNull String elementInstanceIdentifier) {
        if (!ELEMENT_MEMORIES.containsKey(Objects.requireNonNull(elementInstanceIdentifier))) {
            ELEMENT_MEMORIES.put(elementInstanceIdentifier, new RuntimePropertyContainer());
        }
        return ELEMENT_MEMORIES.get(elementInstanceIdentifier);
    }

    public static void clearMemories() {
        ELEMENT_MEMORIES.clear();
    }

}
