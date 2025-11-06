package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface IMixinKeyboardHandler {

    @Invoker("keyPress") void invoke_keyPress_FancyMenu(long window, int action, KeyEvent event);

}
