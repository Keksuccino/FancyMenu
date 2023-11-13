package de.keksuccino.fancymenu.util.resources.audio;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.function.BiConsumer;

public class MinecraftSoundSettingsObserver {

    private static final Map<Long, BiConsumer<SoundSource, Float>> VOLUME_LISTENERS = new HashMap<>();
    private static final Map<Long, Runnable> SOUND_ENGINE_RELOAD_LISTENERS = new HashMap<>();

    private static long idCountVolumeListeners = 0L;
    private static long idCountEngineReloadListeners = 0L;

    /**
     * Registers the listener and returns the ID of the listener, to be able to unregister it later.<br>
     * Volume listeners get notified after the volume of a {@link SoundSource} got changed.
     */
    public static long registerVolumeListener(@NotNull BiConsumer<SoundSource, Float> listener) {
        Objects.requireNonNull(listener);
        idCountVolumeListeners++;
        VOLUME_LISTENERS.put(idCountVolumeListeners, listener);
        return idCountVolumeListeners;
    }

    public static void unregisterVolumeListener(long id) {
        VOLUME_LISTENERS.remove(id);
    }

    public static List<BiConsumer<SoundSource, Float>> getVolumeListeners() {
        return new ArrayList<>(VOLUME_LISTENERS.values());
    }

    /**
     * Registers the listener and returns the ID of the listener, to be able to unregister it later.<br>
     * {@link SoundEngine} reload listeners get notified after {@link SoundEngine#reload()} got called.
     */
    public static long registerSoundEngineReloadListener(@NotNull Runnable listener) {
        Objects.requireNonNull(listener);
        idCountEngineReloadListeners++;
        SOUND_ENGINE_RELOAD_LISTENERS.put(idCountEngineReloadListeners, listener);
        return idCountEngineReloadListeners;
    }

    public static void unregisterSoundEngineReloadListener(long id) {
        SOUND_ENGINE_RELOAD_LISTENERS.remove(id);
    }

    public static List<Runnable> getSoundEngineReloadListeners() {
        return new ArrayList<>(SOUND_ENGINE_RELOAD_LISTENERS.values());
    }

}
