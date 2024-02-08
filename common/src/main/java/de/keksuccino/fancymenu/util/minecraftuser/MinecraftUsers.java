package de.keksuccino.fancymenu.util.minecraftuser;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MinecraftUsers {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> PLAYERS = Collections.synchronizedMap(new HashMap<>());
    private static final String API_URL_PREFIX = "https://api.mojang.com/users/profiles/minecraft/";
    public static final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> UNKNOWN_USER = new HashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapper.create();

    @NotNull
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getUserTextures(@NotNull String playerName) {
        Objects.requireNonNull(playerName);
        if (!PLAYERS.containsKey(playerName)) {
            InputStream in = null;
            try {
                in = Objects.requireNonNull(WebUtils.openResourceStream(API_URL_PREFIX + playerName));
                MinecraftProfileMetadata minecraftProfileMetadata = OBJECT_MAPPER.readValue(IOUtils.toString(in, StandardCharsets.UTF_8), MinecraftProfileMetadata.class);
                GameProfile gameProfile = new GameProfile(minecraftProfileMetadata.getId(), minecraftProfileMetadata.getName());
                MinecraftSessionService minecraftSessionService = Minecraft.getInstance().getMinecraftSessionService();
                gameProfile = minecraftSessionService.fillProfileProperties(gameProfile, false);
                PLAYERS.put(playerName, minecraftSessionService.getTextures(gameProfile, false));
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
