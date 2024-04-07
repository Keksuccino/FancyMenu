package de.keksuccino.fancymenu.networking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//TODO übernehmen (all from networking package)

public class PacketRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, PacketCodec<?>> CODECS = new HashMap<>();

    private static boolean registrationsAllowed = true;

    /**
     * Register packets here.<br>
     * {@link PacketCodec}s should get registered during mod-init.
     **/
    public static void register(@NotNull PacketCodec<?> codec) {
        if (!registrationsAllowed) throw new RuntimeException("Tried to register PacketCodec too late! PacketCodecs need to get registered as early as possible!");
        if (CODECS.containsKey(Objects.requireNonNull(codec.getPacketIdentifier()))) {
            LOGGER.warn("[FANCYMENU] PacketCodec with identifier '" + codec.getPacketIdentifier() + "' already registered! Overriding codec!");
        }
        CODECS.put(codec.getPacketIdentifier(), codec);
    }

    @NotNull
    public static List<PacketCodec<?>> getCodecs() {
        return new ArrayList<>(CODECS.values());
    }

    @Nullable
    public static PacketCodec<?> getCodec(@NotNull String identifier) {
        return CODECS.get(identifier);
    }

    @SuppressWarnings("all")
    @Nullable
    public static <T extends Packet> PacketCodec<T> getCodecFor(@NotNull T packet) {
        try {
            for (PacketCodec<?> codec : CODECS.values()) {
                if (codec.getType() == packet.getClass()) return (PacketCodec<T>) codec;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get codec for packet!", ex);
        }
        return null;
    }

    public static void endRegistrationPhase() {
        registrationsAllowed = false;
    }

}
