package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * @reason Fire FancyMenu's key listeners after vanilla processing so they run both in menus and during gameplay.
     */
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void triggerKeyListeners_FancyMenu(long window, int action, KeyEvent event, CallbackInfo info) {
        if (window != this.minecraft.getWindow().handle()) {
            return;
        }

        if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_KEY_RELEASED.handleKeyReleased(event.key(), event.scancode(), event.modifiers());
        } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            Listeners.ON_KEY_PRESSED.handleKeyPressed(event.key(), event.scancode(), event.modifiers());
        }
    }

}