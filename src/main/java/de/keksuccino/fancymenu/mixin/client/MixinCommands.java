package de.keksuccino.fancymenu.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.events.CommandsRegisterEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class MixinCommands {

    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;findAmbiguities(Lcom/mojang/brigadier/AmbiguityConsumer;)V"), method = "<init>")
    private void onConstruct(Commands.CommandSelection commandSelection, CallbackInfo info) {

        Konkrete.getEventHandler().callEventsFor(new CommandsRegisterEvent(this.dispatcher));

    }

}
