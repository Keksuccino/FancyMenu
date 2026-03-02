package de.keksuccino.fancymenu.fmdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FmDataServerListenerHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final File LISTENERS_FILE = new File(FancyMenu.MOD_DIR, "fmdata_server_listeners.json");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Map<String, FmDataServerListener> LISTENERS = new LinkedHashMap<>();

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        readFromFile();
        initialized = true;
    }

    public static synchronized boolean addListener(@NotNull FmDataServerListener listener) {
        init();
        listener.normalize();
        if (!listener.hasValidName() || !listener.hasCommands()) {
            return false;
        }

        String key = key(listener.listener_name);
        if (LISTENERS.containsKey(key)) {
            return false;
        }

        LISTENERS.put(key, listener);
        writeToFile();
        return true;
    }

    public static synchronized boolean editListener(@NotNull String listenerName, @NotNull FmDataServerListener editedListener) {
        init();

        String key = key(listenerName);
        FmDataServerListener existing = LISTENERS.get(key);
        if (existing == null) {
            return false;
        }

        editedListener.normalize();
        editedListener.listener_name = existing.listener_name;
        if (!editedListener.hasCommands()) {
            return false;
        }

        LISTENERS.put(key, editedListener);
        writeToFile();
        return true;
    }

    public static synchronized boolean removeListener(@NotNull String listenerName) {
        init();
        String key = key(listenerName);
        FmDataServerListener removed = LISTENERS.remove(key);
        if (removed == null) {
            return false;
        }
        writeToFile();
        return true;
    }

    @Nullable
    public static synchronized FmDataServerListener getListener(@NotNull String listenerName) {
        init();
        return LISTENERS.get(key(listenerName));
    }

    @NotNull
    public static synchronized List<FmDataServerListener> getListeners() {
        init();
        List<FmDataServerListener> listeners = new ArrayList<>(LISTENERS.values());
        listeners.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.listener_name, b.listener_name));
        return listeners;
    }

    @NotNull
    public static synchronized String[] getListenerNameSuggestions() {
        return getListeners().stream().map(listener -> listener.listener_name).toArray(String[]::new);
    }

    public static int onClientDataReceived(@NotNull ServerPlayer sender, @NotNull String dataIdentifier, @NotNull String data) {
        init();

        List<FmDataServerListener> listeners = getListeners();
        int fired = 0;
        for (FmDataServerListener listener : listeners) {
            try {
                if (!listener.shouldFire(sender, dataIdentifier, data)) {
                    continue;
                }
                fireListenerCommands(sender, data, listener);
                fired++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to fire FMData server listener '{}'", listener.listener_name, ex);
            }
        }
        return fired;
    }

    private static void fireListenerCommands(@NotNull ServerPlayer sender, @NotNull String data, @NotNull FmDataServerListener listener) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }
        if ((listener.commands_to_execute_on_fire == null) || listener.commands_to_execute_on_fire.isEmpty()) {
            return;
        }

        for (String command : listener.commands_to_execute_on_fire) {
            if ((command == null) || command.isBlank()) {
                continue;
            }

            try {
                String preparedCommand = command
                        .replace("%fm_sender%", sender.getScoreboardName())
                        .replace("%fm_data%", data);

                if (preparedCommand.isBlank()) {
                    continue;
                }

                CommandSourceStack source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, preparedCommand);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute FMData listener command '{}' for listener '{}'", command, listener.listener_name, ex);
            }
        }
    }

    public static synchronized boolean isListenerNameAlreadyUsed(@NotNull String listenerName) {
        init();
        return LISTENERS.containsKey(key(listenerName));
    }

    private static void writeToFile() {
        try {
            PersistedData persistedData = new PersistedData();
            persistedData.listeners = getListeners();

            Files.createDirectories(LISTENERS_FILE.toPath().getParent());
            Files.writeString(
                    LISTENERS_FILE.toPath(),
                    GSON.toJson(persistedData),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write FMData server listeners file!", ex);
        }
    }

    private static void readFromFile() {
        LISTENERS.clear();

        try {
            if (!LISTENERS_FILE.isFile()) {
                return;
            }

            String json = Files.readString(LISTENERS_FILE.toPath(), StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return;
            }

            PersistedData persistedData = GSON.fromJson(json, PersistedData.class);
            if ((persistedData == null) || (persistedData.listeners == null)) {
                return;
            }

            for (FmDataServerListener listener : persistedData.listeners) {
                if (listener == null) {
                    continue;
                }
                listener.normalize();
                if (!listener.hasValidName() || !listener.hasCommands()) {
                    continue;
                }
                LISTENERS.putIfAbsent(key(listener.listener_name), listener);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read FMData server listeners file!", ex);
        }
    }

    @NotNull
    private static String key(@NotNull String listenerName) {
        return Objects.requireNonNull(listenerName).toLowerCase(Locale.ROOT);
    }

    private static class PersistedData {
        public List<FmDataServerListener> listeners = new ArrayList<>();
    }

}
