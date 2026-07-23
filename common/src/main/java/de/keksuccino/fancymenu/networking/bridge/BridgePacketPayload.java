package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared bridge wire format for every loader.
 * <p>
 * The former Fabric-only {@code minecraft:fancymenu_packet_bridge} identifier is intentionally not retained as an
 * alias. NeoForge rejects mod payload registrations in the {@code minecraft} namespace, so keeping that alias only
 * on Fabric would recreate a loader-specific protocol and make channel negotiation ambiguous again.
 */
public final class BridgePacketPayload implements CustomPacketPayload {

    public static final Type<BridgePacketPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fancymenu", "fancymenu_bridge_packet"));
    public static final StreamCodec<FriendlyByteBuf, BridgePacketPayload> CODEC = CustomPacketPayload.codec(BridgePacketPayload::write, BridgePacketPayload::new);
    public static final String TO_SERVER_WIRE_DIRECTION = "server";
    public static final String TO_CLIENT_WIRE_DIRECTION = "client";

    /**
     * This field is retained for wire compatibility only. Receivers must use the direction supplied by their
     * loader callback, because a remote peer controls the serialized value.
     */
    private final String direction;
    private final String dataWithIdentifier;

    public BridgePacketPayload(@NotNull String direction, @NotNull String dataWithIdentifier) {
        this.direction = direction;
        this.dataWithIdentifier = dataWithIdentifier;
    }

    private BridgePacketPayload(FriendlyByteBuf byteBuf) {
        this(byteBuf.readUtf(), byteBuf.readUtf());
    }

    private void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeUtf(this.direction);
        byteBuf.writeUtf(this.dataWithIdentifier);
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction) {
        if (this.dataWithIdentifier != null) PacketHandler.onPacketReceived(sender, direction, this.dataWithIdentifier);
    }

    public String direction() {
        return this.direction;
    }

    public String dataWithIdentifier() {
        return this.dataWithIdentifier;
    }

    @Override
    public @NotNull Type<BridgePacketPayload> type() {
        return TYPE;
    }

}
