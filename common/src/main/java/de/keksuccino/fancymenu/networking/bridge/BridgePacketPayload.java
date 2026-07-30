package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared bridge message and wire format used by the legacy Fabric and Forge networking adapters. */
public final class BridgePacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("fancymenu", "fancymenu_bridge_packet");
    public static final String TO_SERVER_WIRE_DIRECTION = "server";
    public static final String TO_CLIENT_WIRE_DIRECTION = "client";

    /** Retained for wire compatibility only; receivers must trust their loader callback direction instead. */
    private final String direction;
    private final String dataWithIdentifier;

    public BridgePacketPayload(@NotNull String direction, @NotNull String dataWithIdentifier) {
        this.direction = direction;
        this.dataWithIdentifier = dataWithIdentifier;
    }

    public static @NotNull BridgePacketPayload read(@NotNull FriendlyByteBuf byteBuf) {
        return new BridgePacketPayload(byteBuf.readUtf(), byteBuf.readUtf());
    }

    public void write(@NotNull FriendlyByteBuf byteBuf) {
        byteBuf.writeUtf(this.direction);
        byteBuf.writeUtf(this.dataWithIdentifier);
    }

    public @NotNull FriendlyByteBuf writeToNewBuffer() {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        this.write(byteBuf);
        return byteBuf;
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction) {
        PacketHandler.onPacketReceived(sender, direction, this.dataWithIdentifier);
    }

    public @NotNull String direction() {
        return this.direction;
    }

    public @NotNull String dataWithIdentifier() {
        return this.dataWithIdentifier;
    }
}
