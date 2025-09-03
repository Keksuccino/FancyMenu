package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ListenerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final File LISTENERS_FILE = new File(FancyMenu.MOD_DIR, "listener_instances.txt");
    private static final Map<String, ListenerInstance> INSTANCES = new HashMap<>();

    public static boolean canRegisterListeners = true;
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            readFromFile();
        }
        initialized = true;
        canRegisterListeners = false;
    }

    private static void writeToFile() {
        assertInitialized();
        try {

            PropertyContainerSet instances = new PropertyContainerSet("listener_instances");
            INSTANCES.forEach((s, instance) -> instances.putContainer(instance.serialize()));

            PropertiesParser.serializeSetToFile(instances, LISTENERS_FILE.getAbsolutePath());

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize listener instances to file!", ex);
        }
    }

    private static void readFromFile() {
        INSTANCES.clear();
        try {

            if (!LISTENERS_FILE.isFile()) return;

            PropertyContainerSet instances = Objects.requireNonNull(PropertiesParser.deserializeSetFromFile(LISTENERS_FILE.getAbsolutePath()), "Parser returned NULL as PropertyContainerSet!");

            instances.getContainers().forEach(propertyContainer -> {
                ListenerInstance instance = ListenerInstance.deserialize(propertyContainer);
                if (instance != null) INSTANCES.put(instance.instanceIdentifier, instance);
            });

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instances from file!", ex);
        }
    }

    public static void assertInitialized() {
        if (!initialized) throw new RuntimeException("[FANCYMENU] Tried to access ListenerHandler too early! Not ready yet!");
    }

}
