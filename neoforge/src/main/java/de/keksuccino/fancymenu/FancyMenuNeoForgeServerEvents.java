package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class FancyMenuNeoForgeServerEvents {

    public static void registerAll() {

        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeServerEvents());

    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        ServerOpenGuiScreenCommand.register(e.getDispatcher());
        ServerCloseGuiScreenCommand.register(e.getDispatcher());
        ServerVariableCommand.register(e.getDispatcher());
    }

}
