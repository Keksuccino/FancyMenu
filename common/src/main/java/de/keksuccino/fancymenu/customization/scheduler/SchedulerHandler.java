package de.keksuccino.fancymenu.customization.scheduler;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SchedulerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final File SCHEDULERS_FILE = new File(FancyMenu.MOD_DIR, "scheduler_instances.txt");
    public static final long DEFAULT_TICK_DELAY_MS = 50L;
    private static final CharacterFilter IDENTIFIER_NAME_VALIDATOR = CharacterFilter.buildResourceNameFilter();
    private static final Map<String, SchedulerInstance> INSTANCES = new ConcurrentHashMap<>();
    private static final Map<String, SchedulerRunState> RUNNING = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-Scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private static final Object LOCK = new Object();

    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            readFromFile();
            initialized = true;
            startSchedulersOnLaunch();
        }
    }

    public static void addInstance(@NotNull SchedulerInstance instance) {
        assertInitialized();
        SchedulerInstance ensured = ensureValidIdentifier(instance);
        INSTANCES.put(ensured.getIdentifier(), ensured);
        writeToFile();
    }

    public static void removeInstance(@NotNull String identifier) {
        assertInitialized();
        stopScheduler(identifier);
        INSTANCES.remove(identifier);
        writeToFile();
    }

    public static void applyChanges(@NotNull List<SchedulerInstance> instances) {
        assertInitialized();
        synchronized (LOCK) {
            List<String> toRemove = new ArrayList<>(INSTANCES.keySet());
            INSTANCES.clear();
            for (SchedulerInstance instance : instances) {
                SchedulerInstance ensured = ensureValidIdentifier(instance);
                INSTANCES.put(ensured.getIdentifier(), ensured);
                toRemove.remove(ensured.getIdentifier());
            }
            toRemove.forEach(SchedulerHandler::stopSchedulerInternal);
        }
        writeToFile();
    }

    public static void syncChanges() {
        writeToFile();
    }

    @Nullable
    public static SchedulerInstance getInstance(@NotNull String identifier) {
        assertInitialized();
        return INSTANCES.get(identifier);
    }

    @NotNull
    public static List<SchedulerInstance> getInstances() {
        assertInitialized();
        List<SchedulerInstance> list = new ArrayList<>(INSTANCES.values());
        list.sort(Comparator.comparing(SchedulerInstance::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static boolean isIdentifierValid(@NotNull String identifier) {
        return !identifier.isBlank() && IDENTIFIER_NAME_VALIDATOR.isAllowedText(identifier);
    }

    @NotNull
    public static SchedulerInstance createFreshInstance() {
        return new SchedulerInstance(generateUniqueIdentifier());
    }

    @NotNull
    public static String generateUniqueIdentifier() {
        String base = "scheduler_" + java.util.UUID.randomUUID();
        String identifier = base;
        int index = 1;
        while (INSTANCES.containsKey(identifier)) {
            identifier = base + "_" + index;
            index++;
        }
        return identifier;
    }

    public static void startScheduler(@NotNull String identifier) {
        assertInitialized();
        if (isRunning(identifier)) {
            return;
        }
        SchedulerInstance instance = INSTANCES.get(identifier);
        if (instance == null) {
            LOGGER.warn("[FANCYMENU] Tried to start scheduler but no scheduler with identifier '{}' was found.", identifier);
            return;
        }
        long startDelayMs = Math.max(0L, instance.getStartDelayMs());
        long tickDelayMs = Math.max(0L, instance.getTickDelayMs());
        long ticksToRun = instance.getTicksToRun();
        long scheduleDelay = (tickDelayMs <= 0L) ? DEFAULT_TICK_DELAY_MS : tickDelayMs;
        SchedulerRunState state = new SchedulerRunState(instance, ticksToRun);

        synchronized (LOCK) {
            stopSchedulerInternal(identifier);
            RUNNING.put(identifier, state);
        }

        ScheduledFuture<?> future = EXECUTOR.scheduleWithFixedDelay(() -> runSchedulerTick(identifier, state), startDelayMs, scheduleDelay, TimeUnit.MILLISECONDS);
        state.future = future;
        if (!state.active.get()) {
            future.cancel(false);
        }
    }

    public static void stopScheduler(@NotNull String identifier) {
        synchronized (LOCK) {
            stopSchedulerInternal(identifier);
        }
    }

    public static boolean isRunning(@NotNull String identifier) {
        SchedulerInstance instance = INSTANCES.get(identifier);
        if (instance != null) {
            return isRunning(instance);
        }
        SchedulerRunState state = RUNNING.get(identifier);
        return (state != null) && state.active.get();
    }

    public static boolean isRunning(@NotNull SchedulerInstance instance) {
        return findStateByInstance(instance) != null;
    }

    private static void stopSchedulerInternal(@NotNull String identifier) {
        SchedulerRunState state = RUNNING.remove(identifier);
        if (state != null) {
            state.active.set(false);
            if (state.future != null) {
                state.future.cancel(false);
            }
        }
    }

    @Nullable
    private static SchedulerRunState findStateByInstance(@NotNull SchedulerInstance instance) {
        for (SchedulerRunState state : RUNNING.values()) {
            if (state.active.get() && (state.instance == instance)) {
                return state;
            }
        }
        return null;
    }

    private static void runSchedulerTick(@NotNull String identifier, @NotNull SchedulerRunState state) {
        if (!state.active.get()) {
            return;
        }
        if (state.remainingTicks.get() == 0) {
            stopScheduler(identifier);
            return;
        }
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                if (!state.active.get()) {
                    return;
                }
                state.instance.getActionScript().execute();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Error while trying to execute action script of scheduler '{}'", identifier, ex);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        if (state.remainingTicks.get() > 0 && state.remainingTicks.decrementAndGet() <= 0) {
            stopScheduler(identifier);
        }
    }

    private static void startSchedulersOnLaunch() {
        for (SchedulerInstance instance : INSTANCES.values()) {
            if (instance.isStartOnLaunch()) {
                startScheduler(instance.getIdentifier());
            }
        }
    }

    private static SchedulerInstance ensureValidIdentifier(@NotNull SchedulerInstance instance) {
        String identifier = instance.getIdentifier();
        if (!isIdentifierValid(identifier)) {
            identifier = generateUniqueIdentifier();
        }
        if (INSTANCES.containsKey(identifier) && (INSTANCES.get(identifier) != instance)) {
            identifier = generateUniqueIdentifier();
        }
        instance.setIdentifier(identifier);
        return instance;
    }

    private static void writeToFile() {
        assertInitialized();
        try {
            PropertyContainerSet instances = new PropertyContainerSet("scheduler_instances");
            INSTANCES.forEach((s, instance) -> instances.putContainer(instance.serialize()));
            PropertiesParser.serializeSetToFile(instances, SCHEDULERS_FILE.getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize scheduler instances to file!", ex);
        }
    }

    private static void readFromFile() {
        INSTANCES.clear();
        try {
            if (!SCHEDULERS_FILE.isFile()) return;
            PropertyContainerSet instances = Objects.requireNonNull(PropertiesParser.deserializeSetFromFile(SCHEDULERS_FILE.getAbsolutePath()), "Parser returned NULL as PropertyContainerSet!");
            instances.getContainers().forEach(propertyContainer -> {
                SchedulerInstance instance = SchedulerInstance.deserialize(propertyContainer);
                if (instance != null) {
                    INSTANCES.put(instance.getIdentifier(), instance);
                }
            });
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize scheduler instances from file!", ex);
        }
    }

    public static void assertInitialized() {
        if (!initialized) throw new RuntimeException("[FANCYMENU] Tried to access SchedulerHandler too early! Not ready yet!");
    }

    private static class SchedulerRunState {
        private final SchedulerInstance instance;
        private final AtomicLong remainingTicks;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile ScheduledFuture<?> future;

        private SchedulerRunState(@NotNull SchedulerInstance instance, long ticksToRun) {
            this.instance = instance;
            this.remainingTicks = new AtomicLong((ticksToRun <= 0L) ? -1L : ticksToRun);
        }
    }

}
