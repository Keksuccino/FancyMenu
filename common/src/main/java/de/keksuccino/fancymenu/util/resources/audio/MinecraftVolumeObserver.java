package de.keksuccino.fancymenu.util.resources.audio;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MinecraftVolumeObserver {

    private static final Map<Long, Consumer<Float>> MASTER_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> MUSIC_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> RECORDS_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> WEATHER_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> BLOCKS_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> HOSTILE_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> NEUTRAL_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> PLAYERS_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> AMBIENT_LISTENERS = new HashMap<>();
    private static final Map<Long, Consumer<Float>> VOICE_LISTENERS = new HashMap<>();

    private static long idCount = 0L;

    public static long registerListener(@NotNull SoundSource soundSource, @NotNull Consumer<Float> listener) {
        Objects.requireNonNull(soundSource);
        Objects.requireNonNull(listener);
        idCount++;
        getListenerMap(soundSource).put(idCount, listener);
        return idCount;
    }

    public static void unregisterListener(long id) {
        for (SoundSource soundSource : SoundSource.values()) {
            getListenerMap(soundSource).remove(id);
        }
    }

    @NotNull
    public static Map<Long, Consumer<Float>> getListenerMap(@NotNull SoundSource soundSource) {
        Objects.requireNonNull(soundSource);
        if (soundSource == SoundSource.MUSIC) {
            return MUSIC_LISTENERS;
        }
        if (soundSource == SoundSource.RECORDS) {
            return RECORDS_LISTENERS;
        }
        if (soundSource == SoundSource.WEATHER) {
            return WEATHER_LISTENERS;
        }
        if (soundSource == SoundSource.BLOCKS) {
            return BLOCKS_LISTENERS;
        }
        if (soundSource == SoundSource.HOSTILE) {
            return HOSTILE_LISTENERS;
        }
        if (soundSource == SoundSource.NEUTRAL) {
            return NEUTRAL_LISTENERS;
        }
        if (soundSource == SoundSource.PLAYERS) {
            return PLAYERS_LISTENERS;
        }
        if (soundSource == SoundSource.AMBIENT) {
            return AMBIENT_LISTENERS;
        }
        if (soundSource == SoundSource.VOICE) {
            return VOICE_LISTENERS;
        }
        return MASTER_LISTENERS;
    }

}
