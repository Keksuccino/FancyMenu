package de.keksuccino.fancymenu.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.events.CommandsRegisterEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.server.command.AdvancementCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementCommand.class)
public class MixinAdvancementCommand {

    @Inject(at = @At("HEAD"), method = "register")
    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo info) {

        Konkrete.getEventHandler().callEventsFor(new CommandsRegisterEvent(dispatcher));

    }

}
