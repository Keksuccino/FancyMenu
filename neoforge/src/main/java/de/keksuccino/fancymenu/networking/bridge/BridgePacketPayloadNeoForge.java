package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.PacketPayloadBaseNeoForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BridgePacketPayloadNeoForge extends PacketPayloadBaseNeoForge implements CustomPacketPayload {

    public static final Type<BridgePacketPayloadNeoForge> TYPE = CustomPacketPayload.createType("fancymenu_packet_bridge");
    public static final StreamCodec<FriendlyByteBuf, BridgePacketPayloadNeoForge> CODEC = CustomPacketPayload.codec(BridgePacketPayloadNeoForge::write, BridgePacketPayloadNeoForge::new);

    public String dataWithIdentifier;

    public BridgePacketPayloadNeoForge(@NotNull String direction, @NotNull String dataWithIdentifier) {
        this.direction = direction;
        this.dataWithIdentifier = dataWithIdentifier;
    }

    public BridgePacketPayloadNeoForge(FriendlyByteBuf byteBuf) {
        this(
                byteBuf.readUtf(), //direction
                byteBuf.readUtf() //dataWithIdentifier
        );
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeUtf(this.direction);
        byteBuf.writeUtf(this.dataWithIdentifier);
    }

    public void handle(@Nullable ServerPlayer sender, PacketHandler.PacketDirection direction) {
        if (this.dataWithIdentifier != null) {
            PacketHandler.onPacketReceived(sender, direction, this.dataWithIdentifier);
        }
    }

    @Override
    public @NotNull Type<BridgePacketPayloadNeoForge> type() {
        return TYPE;
    }

}
