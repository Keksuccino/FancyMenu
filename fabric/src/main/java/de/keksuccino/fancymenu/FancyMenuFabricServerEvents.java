package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.WebUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

        ServerLifecycleEvents.SERVER_STARTING.register(PacketHandler::onServerStarting);

        // STOPPING clears networking state before player teardown; the repeated STOPPED cleanup is an intentional,
        // idempotent fallback for partial shutdown paths. An integrated server can stop while its physical client
        // keeps running, so only a dedicated-server process owns the shared internet availability monitor lifecycle.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PacketHandler.onServerStopped(server);
            if (!Services.PLATFORM.isOnClient()) WebUtils.shutdown();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(PacketHandler::onServerStopped);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PacketHandler.onServerPlayerConnected(handler.getPlayer()));

        // Clients register their channels only after JOIN, and Fabric applies that registration on the server thread right before
        // firing this event. This is the first moment FancyMenu's channel is guaranteed to be sendable, so the handshake waits for it.
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(BridgePacketPayload.ID)) PacketHandler.sendHandshakeToClient(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PacketHandler.onServerPlayerDisconnected(handler.getPlayer()));

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> Commands.registerAll(dispatcher));
    }

}
