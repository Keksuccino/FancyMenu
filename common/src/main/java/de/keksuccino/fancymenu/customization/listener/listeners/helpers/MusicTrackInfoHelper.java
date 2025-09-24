package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public final class MusicTrackInfoHelper {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation MUSIC_TRACK_METADATA_LOCATION = ResourceLocation.fromNamespaceAndPath("fancymenu", "metadata/minecraft_music_tracks.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String cachedMusicTrackMetadataJsonString = null;
    private static List<MusicTrackInfo> cachedMusicTrackInfo = null;

    private MusicTrackInfoHelper() {}

    @NotNull
    public static String getMusicTrackMetadataString() {
        InputStream in = null;
        try {
            if (cachedMusicTrackMetadataJsonString == null) {
                in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MUSIC_TRACK_METADATA_LOCATION).open();
                final StringBuilder builder = new StringBuilder();
                FileUtils.readTextLinesFrom(in).forEach(s -> builder.append(s).append("\n"));
                cachedMusicTrackMetadataJsonString = builder.toString();
            }
        } catch (Exception ex) {
            cachedMusicTrackMetadataJsonString = "";
            LOGGER.error("[FANCYMENU] Failed to read Minecraft music track metadata from file!", ex);
        }
        CloseableUtils.closeQuietly(in);
        return (cachedMusicTrackMetadataJsonString != null) ? cachedMusicTrackMetadataJsonString : "";
    }

    @NotNull
    public static List<MusicTrackInfo> getInfoForAllMusicTracks() {
        try {
            if (cachedMusicTrackInfo == null) {
                cachedMusicTrackInfo = GSON.fromJson(getMusicTrackMetadataString(), ---type token here---);
            }
        } catch (Exception ex) {
            cachedMusicTrackInfo = List.of();
            LOGGER.error("[FANCYMENU] Failed to parse Minecraft music track info from Json!", ex);
        }
        return (cachedMusicTrackInfo != null) ? cachedMusicTrackInfo : List.of();
    }

    @Nullable
    public static Component resolveDisplayName(@Nullable String trackResourceLocation, @Nullable String eventResourceLocation) {
        ResourceLocation eventLocation = parseResourceLocation(eventResourceLocation);
        Component component = resolveFromSoundManager(eventLocation);
        if (component != null) {
            return component;
        }

        component = resolveFromRegistries(eventLocation);
        if (component != null) {
            return component;
        }

        ResourceLocation trackLocation = parseResourceLocation(trackResourceLocation);
        if (!Objects.equals(trackLocation, eventLocation)) {
            Component altComponent = resolveFromSoundManager(trackLocation);
            if (altComponent != null) {
                return altComponent;
            }
            altComponent = resolveFromRegistries(trackLocation);
            if (altComponent != null) {
                return altComponent;
            }
        }

        return null;
    }

    @Nullable
    public static String serializeComponent(@Nullable Component component) {
        if (component == null) {
            return null;
        }
        RegistryAccess registryAccess = getCurrentRegistryAccess();
        return Component.Serializer.toJson(component, registryAccess);
    }

    @Nullable
    private static Component resolveFromSoundManager(@Nullable ResourceLocation resourceLocation) {
        if (resourceLocation == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        SoundManager soundManager = minecraft.getSoundManager();
        WeighedSoundEvents events = soundManager.getSoundEvent(resourceLocation);
        if (events == null) {
            return null;
        }
        return events.getSubtitle();
    }

    @Nullable
    private static Component resolveFromRegistries(@Nullable ResourceLocation resourceLocation) {
        RegistryAccess registryAccess = getCurrentRegistryAccess();
        Registry<JukeboxSong> registry = registryAccess.registry(Registries.JUKEBOX_SONG).orElse(null);
        if (registry == null || resourceLocation == null) {
            return null;
        }
        return registry.stream()
                .map(song -> matchSong(song, resourceLocation))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static Component matchSong(@NotNull JukeboxSong song, @NotNull ResourceLocation resourceLocation) {
        if (!song.soundEvent().isBound()) {
            return null;
        }
        SoundEvent soundEvent = song.soundEvent().value();
        if (soundEvent.getLocation().equals(resourceLocation)) {
            return song.description();
        }
        return null;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(@Nullable String trackResourceLocation) {
        if ((trackResourceLocation == null) || trackResourceLocation.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(trackResourceLocation);
    }

    @NotNull
    private static RegistryAccess getCurrentRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.level != null) && (minecraft.level.registryAccess() != null)) {
            return minecraft.level.registryAccess();
        }
        if ((minecraft.getConnection() != null) && (minecraft.getConnection().registryAccess() != null)) {
            return minecraft.getConnection().registryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    /**
     * Example Json entry:
     *
     * {
     *     "resource_location": "assets/minecraft/sounds/music/game/a_familiar_room.ogg",
     *     "display_name": "A Familiar Room",
     *     "artist": "Aaron Cherof",
     *     "duration": "4:07"
     *   }
     */
    public static class MusicTrackInfo {
        public String resource_location; // the track resource location formatted as full asset path, like "assets/minecraft/sounds/music/menu/mutation.ogg"
        public String display_name; // the track display name
        public String artist; // the artist display name
        public String duration; // something like "5:34" (minutes:seconds)
    }

}
