package de.keksuccino.fancymenu.customization.fmdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.fmdata.FmDataToClientPacket;
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

public class FmDataWelcomeDataHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final File WELCOME_DATA_FILE = new File(FancyMenu.MOD_DIR, "fmdata_welcome_data.json");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Map<String, FmDataWelcomeData> WELCOME_DATA = new LinkedHashMap<>();

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        readFromFile();
        initialized = true;
    }

    public static synchronized boolean addWelcomeData(@NotNull FmDataWelcomeData welcomeData) {
        init();
        welcomeData.normalize();
        if (!welcomeData.hasValidName() || !welcomeData.hasValidTargetSelector()) {
            return false;
        }

        String key = key(welcomeData.welcome_data_name);
        if (WELCOME_DATA.containsKey(key)) {
            return false;
        }

        WELCOME_DATA.put(key, welcomeData);
        writeToFile();
        return true;
    }

    public static synchronized boolean editWelcomeData(@NotNull String welcomeDataName, @NotNull FmDataWelcomeData editedWelcomeData) {
        init();

        String key = key(welcomeDataName);
        FmDataWelcomeData existing = WELCOME_DATA.get(key);
        if (existing == null) {
            return false;
        }

        editedWelcomeData.normalize();
        editedWelcomeData.welcome_data_name = existing.welcome_data_name;
        if (!editedWelcomeData.hasValidTargetSelector()) {
            return false;
        }

        WELCOME_DATA.put(key, editedWelcomeData);
        writeToFile();
        return true;
    }

    public static synchronized boolean removeWelcomeData(@NotNull String welcomeDataName) {
        init();
        String key = key(welcomeDataName);
        FmDataWelcomeData removed = WELCOME_DATA.remove(key);
        if (removed == null) {
            return false;
        }
        writeToFile();
        return true;
    }

    @Nullable
    public static synchronized FmDataWelcomeData getWelcomeData(@NotNull String welcomeDataName) {
        init();
        return WELCOME_DATA.get(key(welcomeDataName));
    }

    @NotNull
    public static synchronized List<FmDataWelcomeData> getWelcomeDataEntries() {
        init();
        List<FmDataWelcomeData> entries = new ArrayList<>(WELCOME_DATA.values());
        entries.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.welcome_data_name, b.welcome_data_name));
        return entries;
    }

    @NotNull
    public static synchronized String[] getWelcomeDataNameSuggestions() {
        return getWelcomeDataEntries().stream().map(entry -> entry.welcome_data_name).toArray(String[]::new);
    }

    public static synchronized boolean isWelcomeDataNameAlreadyUsed(@NotNull String welcomeDataName) {
        init();
        return WELCOME_DATA.containsKey(key(welcomeDataName));
    }

    public static int onFancyMenuClientJoined(@NotNull ServerPlayer joinedPlayer) {
        init();

        MinecraftServer server = joinedPlayer.getServer();
        if (server == null) {
            return 0;
        }

        String sentBy = getSentBy(server);
        int sent = 0;

        for (FmDataWelcomeData welcomeData : getWelcomeDataEntries()) {
            try {
                if (!welcomeData.shouldSendTo(joinedPlayer)) {
                    continue;
                }

                FmDataToClientPacket packet = new FmDataToClientPacket();
                packet.data_identifier = welcomeData.data_identifier;
                packet.data = welcomeData.data;
                packet.sent_by = sentBy;
                PacketHandler.sendToClient(joinedPlayer, packet);
                sent++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to send FMData welcome data '{}' to player '{}'", welcomeData.welcome_data_name, joinedPlayer.getScoreboardName(), ex);
            }
        }

        return sent;
    }

    @NotNull
    private static String getSentBy(@NotNull MinecraftServer server) {
        if (!server.isDedicatedServer()) {
            return "integrated_server";
        }

        String localIp = server.getLocalIp();
        if ((localIp == null) || localIp.isBlank()) {
            return "unknown_server";
        }
        return localIp;
    }

    private static void writeToFile() {
        try {
            PersistedData persistedData = new PersistedData();
            persistedData.welcome_data = getWelcomeDataEntries();

            Files.createDirectories(WELCOME_DATA_FILE.toPath().getParent());
            Files.writeString(
                    WELCOME_DATA_FILE.toPath(),
                    GSON.toJson(persistedData),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write FMData welcome data file!", ex);
        }
    }

    private static void readFromFile() {
        WELCOME_DATA.clear();

        try {
            if (!WELCOME_DATA_FILE.isFile()) {
                return;
            }

            String json = Files.readString(WELCOME_DATA_FILE.toPath(), StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return;
            }

            PersistedData persistedData = GSON.fromJson(json, PersistedData.class);
            if ((persistedData == null) || (persistedData.welcome_data == null)) {
                return;
            }

            for (FmDataWelcomeData entry : persistedData.welcome_data) {
                if (entry == null) {
                    continue;
                }
                entry.normalize();
                if (!entry.hasValidName() || !entry.hasValidTargetSelector()) {
                    continue;
                }
                WELCOME_DATA.putIfAbsent(key(entry.welcome_data_name), entry);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read FMData welcome data file!", ex);
        }
    }

    @NotNull
    private static String key(@NotNull String welcomeDataName) {
        return Objects.requireNonNull(welcomeDataName).toLowerCase(Locale.ROOT);
    }

    private static class PersistedData {
        public List<FmDataWelcomeData> welcome_data = new ArrayList<>();
    }

}
