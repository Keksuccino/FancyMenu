package de.keksuccino.fancymenu.networking;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("all")
public class PacketCodec<T extends Packet> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected final String packetIdentifier;
    @NotNull
    protected final Class<T> type;

    public PacketCodec(@NotNull String packetIdentifier, @NotNull Class<T> type) {
        this.packetIdentifier = Objects.requireNonNull(packetIdentifier);
        this.type = Objects.requireNonNull(type);
    }

    @Nullable
    public String serialize(@NotNull T packet) {
        try {
            Gson gson = this.buildGson();
            return this.getPacketIdentifier() + ":" + Objects.requireNonNull(gson.toJson(Objects.requireNonNull(packet)));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize packet!", ex);
        }
        return null;
    }

    @Nullable
    public T deserialize(@NotNull String dataWithoutIdentifier) {
        try {
            Gson gson = this.buildGson();
            return Objects.requireNonNull(gson.fromJson(Objects.requireNonNull(dataWithoutIdentifier), this.type));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize packet!", ex);
        }
        return null;
    }

    @NotNull
    protected Gson buildGson() {
        return new Gson();
    }

    @NotNull
    public String getPacketIdentifier() {
        return this.packetIdentifier;
    }

    @NotNull
    public Class<T> getType() {
        return this.type;
    }

}
