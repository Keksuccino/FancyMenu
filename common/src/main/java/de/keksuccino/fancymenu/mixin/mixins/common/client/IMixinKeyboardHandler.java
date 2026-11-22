package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface IMixinKeyboardHandler {

    @Invoker("charTyped") void invoke_charTyped_FancyMenu(long window, int codePoint, int modifiers);

}
