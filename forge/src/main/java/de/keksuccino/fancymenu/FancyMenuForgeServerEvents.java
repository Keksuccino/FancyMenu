package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;

public class FancyMenuForgeServerEvents {

    public static void registerAll() {

        RegisterCommandsEvent.BUS.addListener(FancyMenuForgeServerEvents::onRegisterServerCommands);

    }

    private static void onRegisterServerCommands(RegisterCommandsEvent e) {
        Commands.registerAll(e.getDispatcher());
    }

}
