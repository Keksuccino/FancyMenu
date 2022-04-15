package de.keksuccino.fancymenu.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.events.CommandsRegisterEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.AdvancementCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementCommands.class)
public class MixinAdvancementCommand {

    @Inject(at = @At("HEAD"), method = "register")
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo info) {

        Konkrete.getEventHandler().callEventsFor(new CommandsRegisterEvent(dispatcher));

    }

}
