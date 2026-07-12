package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;

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

    public static synchronized void addInstance(@NotNull ListenerInstance instance) {
        assertInitialized();
        ListenerInstance previousInstance = INSTANCES.put(instance.instanceIdentifier, instance);
        if (previousInstance != null && previousInstance.parent != instance.parent) {
            previousInstance.parent.unregisterInstance(previousInstance);
        }
        instance.registerSelfToParent();
        writeToFile();
    }

    public static synchronized void removeInstance(@NotNull String identifier) {
        assertInitialized();
        ListenerInstance instance = INSTANCES.get(identifier);
        if (instance != null) instance.parent.unregisterInstance(instance);
        INSTANCES.remove(identifier);
        writeToFile();
    }

    public static synchronized void replaceInstances(@NotNull Collection<ListenerInstance> replacements) {
        assertInitialized();
        replaceInstancesInternal(replacements);
        writeToFile();
    }

    public static synchronized void syncChanges() {
        writeToFile();
    }

    @Nullable
    public static synchronized ListenerInstance getInstance(@NotNull String identifier) {
        assertInitialized();
        return INSTANCES.get(identifier);
    }

    @NotNull
    public static synchronized List<ListenerInstance> getInstances() {
        assertInitialized();
        return new ArrayList<>(INSTANCES.values());
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
        List<ListenerInstance> loadedInstances = new ArrayList<>();
        try {

            if (!LISTENERS_FILE.isFile()) {
                replaceInstancesInternal(loadedInstances);
                return;
            }

            PropertyContainerSet instances = Objects.requireNonNull(PropertiesParser.deserializeSetFromFile(LISTENERS_FILE.getAbsolutePath()), "Parser returned NULL as PropertyContainerSet!");

            instances.getContainers().forEach(propertyContainer -> {
                ListenerInstance instance = ListenerInstance.deserialize(propertyContainer);
                if (instance != null) {
                    loadedInstances.add(instance);
                }
            });

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize listener instances from file!", ex);
        }
        replaceInstancesInternal(loadedInstances);
    }

    private static void replaceInstancesInternal(@NotNull Collection<ListenerInstance> replacements) {
        Map<String, ListenerInstance> replacementMap = new HashMap<>();
        for (ListenerInstance instance : replacements) {
            replacementMap.put(instance.instanceIdentifier, instance);
        }
        Map<AbstractListener, List<ListenerInstance>> replacementsByProvider = new HashMap<>();
        for (ListenerInstance instance : replacementMap.values()) {
            replacementsByProvider.computeIfAbsent(instance.parent, listener -> new ArrayList<>()).add(instance);
        }
        for (AbstractListener listener : ListenerRegistry.getListeners()) {
            listener.replaceInstances(replacementsByProvider.getOrDefault(listener, List.of()));
        }
        INSTANCES.clear();
        INSTANCES.putAll(replacementMap);
    }

    public static void assertInitialized() {
        if (!initialized) throw new RuntimeException("[FANCYMENU] Tried to access ListenerHandler too early! Not ready yet!");
    }

}
