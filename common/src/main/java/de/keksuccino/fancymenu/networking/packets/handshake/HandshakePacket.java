package de.keksuccino.fancymenu.networking.packets.handshake;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.keksuccino.fancymenu.networking.Packet;
import de.keksuccino.fancymenu.networking.bridge.BridgeProtocol;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class HandshakePacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable private JsonElement bridgeProtocolVersion;

    /**
     * Gson and older call sites need a no-argument shape. A missing field deliberately means legacy-only.
     */
    public HandshakePacket() {
    }

    private HandshakePacket(int bridgeProtocolVersion) {
        this.bridgeProtocolVersion = new JsonPrimitive(bridgeProtocolVersion);
    }

    public static @NotNull HandshakePacket current() {
        return new HandshakePacket(BridgeProtocol.VERSION);
    }

    public int bridgeProtocolVersion() {
        if (this.bridgeProtocolVersion == null || !this.bridgeProtocolVersion.isJsonPrimitive() || !this.bridgeProtocolVersion.getAsJsonPrimitive().isNumber()) return 0;
        try {
            BigDecimal version = this.bridgeProtocolVersion.getAsBigDecimal().stripTrailingZeros();
            if (version.signum() < 0 || version.scale() > 0 || version.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) return 0;
            return version.intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return 0;
        }
    }

    @Override
    public boolean processClientPacket(@NotNull Connection connection) {
        return ClientSideHandshakePacketLogic.handle(this, connection);
    }

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return false;
        } else {
            return ServerSideHandshakePacketLogic.handle(sender, this);
        }
    }

}
