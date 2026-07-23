package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.WebUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

        // An integrated server can stop while its physical client keeps running, so only a dedicated-server process
        // owns the server lifecycle of the shared internet availability monitor.
        if (!Services.PLATFORM.isOnClient()) ServerLifecycleEvents.SERVER_STOPPING.register(server -> WebUtils.shutdown());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PacketHandler.sendHandshakeToClient(handler.getPlayer());
        });

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> Commands.registerAll(dispatcher));
    }

}
