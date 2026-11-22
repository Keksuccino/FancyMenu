package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets the dev-only decoration overlay preview simulate pointer input through the game's real mouse handlers,
 * which the hidden window of off-screen test clients never gets from the OS.
 */
@Mixin(MouseHandler.class)
public interface AccessorMixinMouseHandler {

    @Invoker("onMove")
    void invoke_onMove_FancyMenu(long windowPointer, double xpos, double ypos);

    @Invoker("onPress")
    void invoke_onPress_FancyMenu(long windowPointer, int button, int action, int modifiers);

    @Invoker("onScroll")
    void invoke_onScroll_FancyMenu(long window, double xOffset, double yOffset);

}
