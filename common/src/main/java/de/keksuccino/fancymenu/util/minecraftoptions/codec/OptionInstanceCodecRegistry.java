package de.keksuccino.fancymenu.util.minecraftoptions.codec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionInstanceCodecRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, OptionInstanceCodec<?>> CODECS = new HashMap<>();

    public static void register(@NotNull OptionInstanceCodec<?> codec) {
        if (codecExists(codec.type)) {
            LOGGER.warn("[FANCYMENU] Overriding existing OptionInstanceCodec: " + codec.type);
        }
        CODECS.put(codec.type, codec);
    }

    @SuppressWarnings("all")
    @Nullable
    public static <T> OptionInstanceCodec<T> getCodec(@NotNull Class<? extends T> type) {
        OptionInstanceCodec<?> codec = CODECS.get(type);
        return (codec != null) ? (OptionInstanceCodec<T>) codec : null;
    }

    @NotNull
    public static List<OptionInstanceCodec<?>> getCodecs() {
        return new ArrayList<>(CODECS.values());
    }

    public static boolean codecExists(@NotNull Class<?> type) {
        return getCodec(type) != null;
    }

}
