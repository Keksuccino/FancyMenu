package de.keksuccino.fancymenu.util.minecraftuser.v2;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MinecraftUsers {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final int PROFILE_API_TIMEOUT_MS = 5000;
    private static final String MOJANG_PROFILE_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Map<String, UserProfile> CACHED_PROFILES = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> CACHED_PROFILE_TEXTURES = Collections.synchronizedMap(new HashMap<>());

    public static final UserProfile UNKNOWN_USER_PROFILE = new UserProfile();
    public static final MinecraftProfileTexture MISSING_SKIN_TEXTURE = new MinecraftProfileTexture("", Collections.emptyMap());
    public static final MinecraftProfileTexture MISSING_CAPE_TEXTURE = new MinecraftProfileTexture("", Collections.emptyMap());
    public static final MinecraftProfileTexture MISSING_ELYTRA_TEXTURE = new MinecraftProfileTexture("", Collections.emptyMap());
    public static final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> MISSING_PROFILE_TEXTURES = Map.of(
            MinecraftProfileTexture.Type.SKIN, MISSING_SKIN_TEXTURE,
            MinecraftProfileTexture.Type.CAPE, MISSING_CAPE_TEXTURE,
            MinecraftProfileTexture.Type.ELYTRA, MISSING_ELYTRA_TEXTURE);

    @NotNull
    public static UserProfile getUserProfile(@NotNull String playerName) {
        Objects.requireNonNull(playerName);

        UserProfile cachedProfile = CACHED_PROFILES.get(playerName);
        if (cachedProfile != null) return cachedProfile;

        HttpURLConnection connection = null;
        InputStream in = null;
        UserProfile profile = UNKNOWN_USER_PROFILE;

        try {
            URL url = new URL(MOJANG_PROFILE_API_URL + playerName);
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/4.0");
            connection.setConnectTimeout(PROFILE_API_TIMEOUT_MS);
            connection.setReadTimeout(PROFILE_API_TIMEOUT_MS);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = connection.getInputStream();
                String json = String.join("", FileUtils.readTextLinesFrom(in));
                if (!json.isBlank()) {
                    UserProfile parsedProfile = GSON.fromJson(json, UserProfile.class);
                    if ((parsedProfile != null) && (parsedProfile.getUUID() != null)) {
                        profile = parsedProfile;
                    }
                }
            } else if ((responseCode != HttpURLConnection.HTTP_NO_CONTENT) && (responseCode != HttpURLConnection.HTTP_NOT_FOUND)) {
                LOGGER.error("[FANCYMENU] Failed to get player profile via Mojang API: " + playerName + " (HTTP " + responseCode + ")");
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get player profile via Mojang API: " + playerName, ex);
        } finally {
            CloseableUtils.closeQuietly(in);
            if (connection != null) {
                connection.disconnect();
            }
        }

        CACHED_PROFILES.put(playerName, profile);

        return profile;

    }

    @NotNull
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getProfileTextures(@NotNull String playerName) {

        Objects.requireNonNull(playerName);

        if (CACHED_PROFILE_TEXTURES.containsKey(playerName)) return CACHED_PROFILE_TEXTURES.get(playerName);

        try {

            UserProfile profile = getUserProfile(playerName);

            if (profile != UNKNOWN_USER_PROFILE) {
                GameProfile gameProfile = new GameProfile(Objects.requireNonNull(profile.getUUID()), Objects.requireNonNullElse(profile.getName(), playerName));
                MinecraftSessionService minecraftSessionService = Minecraft.getInstance().getMinecraftSessionService();
                gameProfile = minecraftSessionService.fillProfileProperties(gameProfile, false);
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = Objects.requireNonNull(minecraftSessionService.getTextures(gameProfile, false));
                CACHED_PROFILE_TEXTURES.put(playerName, textures);
            } else {
                CACHED_PROFILE_TEXTURES.put(playerName, MISSING_PROFILE_TEXTURES);
            }

        } catch (Exception ex) {
            CACHED_PROFILE_TEXTURES.put(playerName, MISSING_PROFILE_TEXTURES);
            LOGGER.error("[FANCYMENU] Failed to get player skin!", ex);
        }

        return Objects.requireNonNullElse(CACHED_PROFILE_TEXTURES.get(playerName), MISSING_PROFILE_TEXTURES);

    }

    @Nullable
    public static MinecraftProfileTexture getProfileTexture(@NotNull String playerName, @NotNull MinecraftProfileTexture.Type type) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = getProfileTextures(playerName);
        return textures.get(type);
    }

}
