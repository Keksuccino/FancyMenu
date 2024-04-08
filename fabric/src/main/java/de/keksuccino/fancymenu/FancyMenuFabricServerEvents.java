package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> Commands.registerAll(dispatcher));
    }

}
