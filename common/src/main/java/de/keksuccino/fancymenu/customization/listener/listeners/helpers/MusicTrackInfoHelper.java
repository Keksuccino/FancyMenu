package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class MusicTrackInfoHelper {

    private MusicTrackInfoHelper() {}

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
}
