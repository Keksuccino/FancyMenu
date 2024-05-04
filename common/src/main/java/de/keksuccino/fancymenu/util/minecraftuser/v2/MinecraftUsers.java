package de.keksuccino.fancymenu.util.minecraftuser.v2;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.*;

public class MinecraftUsers {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MOJANG_PROFILE_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MINETOOLS_PROFILE_API_URL = "https://api.minetools.eu/uuid/";
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
        return _getUserProfile(playerName, false);
    }

    @NotNull
    private static UserProfile _getUserProfile(@NotNull String playerName, boolean useMojangApi) {

        Objects.requireNonNull(playerName);

        if (CACHED_PROFILES.containsKey(playerName)) return CACHED_PROFILES.get(playerName);

        UserProfile profile = null;
        InputStream in = null;

        try {

            Gson gson = new Gson();
            in = Objects.requireNonNull(WebUtils.openResourceStream(useMojangApi ? (MOJANG_PROFILE_API_URL + playerName) : (MINETOOLS_PROFILE_API_URL + playerName)));
            List<String> jsonLines = FileUtils.readTextLinesFrom(in);
            StringBuilder json = new StringBuilder();
            jsonLines.forEach(json::append);
            profile = Objects.requireNonNull(gson.fromJson(json.toString(), UserProfile.class));
            CACHED_PROFILES.put(playerName, profile);

        } catch (Exception ex) {
            if (!useMojangApi) {
                LOGGER.error("[FANCYMENU] Failed to get player profile '" + playerName + "' via Minetools API! Trying Mojang API now..", ex);
                return _getUserProfile(playerName, true);
            }
            CACHED_PROFILES.put(playerName, UNKNOWN_USER_PROFILE);
            LOGGER.error("[FANCYMENU] Failed to get player profile: " + playerName, ex);
        }

        CloseableUtils.closeQuietly(in);

        return (profile != null) ? profile : UNKNOWN_USER_PROFILE;

    }

    @NotNull
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getProfileTextures(@NotNull String playerName) {

        Objects.requireNonNull(playerName);

        if (CACHED_PROFILE_TEXTURES.containsKey(playerName)) return CACHED_PROFILE_TEXTURES.get(playerName);

        try {

            UserProfile profile = getUserProfile(playerName);

            if (profile != UNKNOWN_USER_PROFILE) {
                GameProfile gameProfile = new GameProfile(Objects.requireNonNull(profile.getUUID()), Objects.requireNonNull(profile.getName()));
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
