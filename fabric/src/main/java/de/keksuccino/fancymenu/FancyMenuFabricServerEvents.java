package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

    }

    //TODO übernehmen
    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> Commands.registerAll(dispatcher));
    }

}
