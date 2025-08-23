package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import de.keksuccino.fancymenu.networking.PacketHandler;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PacketHandler.sendHandshakeToClient(handler.getPlayer());
        });

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> Commands.registerAll(dispatcher));
    }

}
