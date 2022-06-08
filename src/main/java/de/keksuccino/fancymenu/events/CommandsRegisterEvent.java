package de.keksuccino.fancymenu.events;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.commands.CommandSourceStack;

public class CommandsRegisterEvent extends EventBase {

    private CommandDispatcher<CommandSourceStack> dispatcher;

    public CommandsRegisterEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
