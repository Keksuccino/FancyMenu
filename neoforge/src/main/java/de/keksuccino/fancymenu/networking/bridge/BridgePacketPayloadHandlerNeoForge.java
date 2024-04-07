package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BridgePacketPayloadHandlerNeoForge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final BridgePacketPayloadHandlerNeoForge INSTANCE = new BridgePacketPayloadHandlerNeoForge();

    public static BridgePacketPayloadHandlerNeoForge getInstance() {
        return INSTANCE;
    }

    public void handleData(final BridgePacketPayloadNeoForge data, PacketHandler.PacketDirection direction, final PlayPayloadContext context) {
        ServerPlayer player = null;
        if (context.player().orElse(null) instanceof ServerPlayer p) player = p;
        PacketHandler.onPacketReceived(player, direction, data.dataWithIdentifier());
    }

}
