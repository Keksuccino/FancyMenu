package de.keksuccino.fancymenu.events;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.server.command.ServerCommandSource;

public class CommandsRegisterEvent extends EventBase {

    private CommandDispatcher<ServerCommandSource> dispatcher;

    public CommandsRegisterEvent(CommandDispatcher<ServerCommandSource> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public CommandDispatcher<ServerCommandSource> getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
