package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utility that exposes FancyMenu\'s Minecraft music metadata for listeners.
 */
public final class MusicTrackInfoHelper {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation MUSIC_TRACK_METADATA_LOCATION = new ResourceLocation("fancymenu", "metadata/minecraft_music_tracks.json");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MUSIC_TRACK_INFO_TYPE = new TypeToken<List<MusicTrackInfo>>() {}.getType();

    private static String cachedMusicTrackMetadataJsonString;
    private static List<MusicTrackInfo> cachedMusicTrackInfo;

    private MusicTrackInfoHelper() {}

    /**
     * @return all known music tracks, loading and caching the JSON payload on demand.
     */
    @NotNull
    public static List<MusicTrackInfo> getInfoForAllMusicTracks() {
        if (cachedMusicTrackInfo == null) {
            try {
                String json = getMusicTrackMetadataString();
                List<MusicTrackInfo> parsed = GSON.fromJson(json, MUSIC_TRACK_INFO_TYPE);
                if (parsed == null) {
                    parsed = Collections.emptyList();
                }
                parsed.forEach(MusicTrackInfo::initialize);
                cachedMusicTrackInfo = Collections.unmodifiableList(parsed);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to parse Minecraft music track metadata!", ex);
                cachedMusicTrackInfo = Collections.emptyList();
            }
        }
        return cachedMusicTrackInfo;
    }

    /**
     * Locates track metadata using the resolved audio resource (preferred) or the backing sound event.
     */
    @Nullable
    public static MusicTrackInfo findTrackInfo(@Nullable String trackResourceLocation, @Nullable String eventResourceLocation) {
        String normalizedTrack = normalizeResourceLocation(trackResourceLocation);
        String normalizedEvent = normalizeEventLocation(eventResourceLocation);

        MusicTrackInfo fallback = null;
        for (MusicTrackInfo info : getInfoForAllMusicTracks()) {
            if (info.matchesTrack(normalizedTrack)) {
                return info;
            }
            if (fallback == null && info.matchesEvent(normalizedEvent)) {
                fallback = info;
            }
        }
        return fallback;
    }

    /**
     * Attempts to turn a {@link Sound} into the canonical resource path string used in metadata.
     */
    @Nullable
    public static String extractTrackResourceLocation(@Nullable Sound sound) {
        if (sound == null || sound == SoundManager.EMPTY_SOUND) {
            return null;
        }
        ResourceLocation path = sound.getPath();
        if (path != null) {
            return path.toString();
        }
        ResourceLocation fallback = sound.getLocation();
        return fallback != null ? fallback.toString() : null;
    }

    @NotNull
    private static String getMusicTrackMetadataString() {
        if (cachedMusicTrackMetadataJsonString != null) {
            return cachedMusicTrackMetadataJsonString;
        }
        InputStream in = null;
        try {
            in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MUSIC_TRACK_METADATA_LOCATION).open();
            StringBuilder builder = new StringBuilder();
            FileUtils.readTextLinesFrom(in).forEach(line -> builder.append(line).append('\n'));
            cachedMusicTrackMetadataJsonString = builder.toString();
        } catch (Exception ex) {
            cachedMusicTrackMetadataJsonString = "";
            LOGGER.error("[FANCYMENU] Failed to read Minecraft music track metadata from file!", ex);
        }
        CloseableUtils.closeQuietly(in);
        return cachedMusicTrackMetadataJsonString;
    }

    private static String normalizeResourceLocation(@Nullable String location) {
        if (location == null) return null;
        String value = location.replace('\\', '/').trim();
        if (value.isEmpty()) return null;
        value = stripPrefix(value, "minecraft:");
        value = stripPrefix(value, "assets/");
        value = stripPrefix(value, "minecraft/");
        value = stripPrefix(value, "sounds/");
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeEventLocation(@Nullable String location) {
        if (location == null) return null;
        String value = location.trim();
        if (value.isEmpty()) return null;
        if (value.startsWith("minecraft:")) {
            value = value.substring("minecraft:".length());
        }
        value = value.replace('.', '/');
        return value.toLowerCase(Locale.ROOT);
    }

    private static String stripPrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /** Holds metadata for one track. */
    public static final class MusicTrackInfo {
        public String resource_location;
        public String display_name;
        public String artist;
        public String duration;

        private String normalizedResourcePath;
        private long durationMillis;

        private void initialize() {
            this.normalizedResourcePath = normalizeResourceLocation(this.resource_location);
            this.durationMillis = parseDurationMillis(this.duration);
            if (this.display_name == null || this.display_name.isBlank()) {
                this.display_name = "Unknown";
            }
            if (this.artist == null || this.artist.isBlank()) {
                this.artist = "Unknown";
            }
        }

        public String getDisplayName() {
            return this.display_name;
        }

        public String getArtist() {
            return this.artist;
        }

        public long getDurationMillis() {
            return this.durationMillis;
        }

        public String getNormalizedResourcePath() {
            return this.normalizedResourcePath;
        }

        private boolean matchesTrack(@Nullable String normalizedTrack) {
            return normalizedTrack != null && this.normalizedResourcePath != null && this.normalizedResourcePath.equals(normalizedTrack);
        }

        private boolean matchesEvent(@Nullable String normalizedEvent) {
            return normalizedEvent != null && this.normalizedResourcePath != null && this.normalizedResourcePath.startsWith(normalizedEvent);
        }

        private static long parseDurationMillis(@Nullable String duration) {
            if (duration == null || duration.isBlank()) {
                return 0L;
            }
            String[] parts = duration.trim().split(":");
            long totalSeconds = 0L;
            try {
                for (String part : parts) {
                    totalSeconds = (totalSeconds * 60L) + Long.parseLong(part.trim());
                }
                return totalSeconds * 1000L;
            } catch (NumberFormatException ex) {
                LOGGER.warn("[FANCYMENU] Invalid duration value in music metadata: {}", duration, ex);
                return 0L;
            }
        }
    }
}
