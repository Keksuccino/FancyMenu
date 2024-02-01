package de.keksuccino.fancymenu.util.minecraftuser;

import com.google.gson.Gson;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.InputStream;
import java.util.*;

//TODO übernehmen
public class MinecraftUsers {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, MinecraftUserMetadata> PLAYERS = Collections.synchronizedMap(new HashMap<>());
    private static final String API_URL_PREFIX = "https://api.ashcon.app/mojang/v2/user/";
    public static final MinecraftUserMetadata UNKNOWN_USER = new MinecraftUserMetadata();

    @NotNull
    public static MinecraftUserMetadata getUserMetadata(@NotNull String playerName) {
        Objects.requireNonNull(playerName);
        if (!PLAYERS.containsKey(playerName)) {
            InputStream in = null;
            try {
                Gson gson = new Gson();
                in = Objects.requireNonNull(WebUtils.openResourceStream(API_URL_PREFIX + playerName));
                List<String> jsonLines = FileUtils.readTextLinesFrom(in);
                StringBuilder json = new StringBuilder();
                jsonLines.forEach(json::append);
                PLAYERS.put(playerName, Objects.requireNonNull(gson.fromJson(json.toString(), MinecraftUserMetadata.class)));
            } catch (Exception ex) {
                PLAYERS.put(playerName, UNKNOWN_USER);
                LOGGER.error("[FANCYMENU] Failed to get metadata of Minecraft user: " + playerName, ex);
            }
            CloseableUtils.closeQuietly(in);
        }
        return Objects.requireNonNullElse(PLAYERS.get(playerName), UNKNOWN_USER);
    }

    public static boolean isUserMetadataCached(@NotNull String playerName) {
        return PLAYERS.containsKey(playerName);
    }

}
