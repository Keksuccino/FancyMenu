package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class FancyMenuFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

    }

    private static void registerServerCommands() {

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {

            ServerOpenGuiScreenCommand.register(dispatcher);
            ServerCloseGuiScreenCommand.register(dispatcher);
            ServerVariableCommand.register(dispatcher);

        });

    }

}
