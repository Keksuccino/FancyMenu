package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.PacketMessageBaseFabric;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BridgePacketPayload extends PacketMessageBaseFabric implements CustomPacketPayload {

    public static final Type<BridgePacketPayload> TYPE = CustomPacketPayload.createType("fancymenu:packet_bridge");
    public static final StreamCodec<FriendlyByteBuf, BridgePacketPayload> CODEC = CustomPacketPayload.codec(BridgePacketPayload::write, BridgePacketPayload::new);

    public String dataWithIdentifier;

    public BridgePacketPayload(@NotNull String direction, @NotNull String dataWithIdentifier) {
        this.direction = direction;
        this.dataWithIdentifier = dataWithIdentifier;
    }

    public BridgePacketPayload(FriendlyByteBuf byteBuf) {
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
    public @NotNull Type<BridgePacketPayload> type() {
        return TYPE;
    }

}
