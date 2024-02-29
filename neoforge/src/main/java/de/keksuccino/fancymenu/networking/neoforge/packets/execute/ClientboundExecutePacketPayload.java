package de.keksuccino.fancymenu.networking.neoforge.packets.execute;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public record ClientboundExecutePacketPayload(@NotNull String command) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("fancymenu", "clientbound_execute_payload");

    public ClientboundExecutePacketPayload(final FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    @Override
    public void write(@NotNull final FriendlyByteBuf buffer) {
        buffer.writeUtf(Objects.requireNonNull(command()));
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

}
