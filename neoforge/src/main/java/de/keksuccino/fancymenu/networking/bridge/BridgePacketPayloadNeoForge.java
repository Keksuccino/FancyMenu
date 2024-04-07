package de.keksuccino.fancymenu.networking.bridge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public record BridgePacketPayloadNeoForge(@NotNull String dataWithIdentifier) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("fancymenu", "bridge_payload");

    public BridgePacketPayloadNeoForge(final FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    @Override
    public void write(@NotNull final FriendlyByteBuf buffer) {
        buffer.writeUtf(Objects.requireNonNull(dataWithIdentifier()));
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

}
