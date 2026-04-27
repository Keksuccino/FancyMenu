package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    /**
     * @reason Fire FancyMenu's key listeners after vanilla processing so they run both in menus and during gameplay.
     */
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void triggerKeyListeners_FancyMenu(long windowPointer, int action, KeyEvent event, CallbackInfo ci) {
        if (windowPointer != WindowHandler.getWindowHandle()) {
            return;
        }

        int key = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_KEY_RELEASED.handleKeyReleased(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyReleased(key, scanCode, modifiers);
        } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            Listeners.ON_KEY_PRESSED.handleKeyPressed(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyPressed(key, scanCode, modifiers, action == GLFW.GLFW_REPEAT);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void head_keyPress_FancyMenu(long windowPointer, int action, KeyEvent event, CallbackInfo info) {
        if (windowPointer == WindowHandler.getWindowHandle()) {
            int key = event.key();
            int scanCode = event.scancode();
            int modifiers = event.modifiers();
            if (action == 1 || action == 2) {
                if (ScreenOverlayHandler.INSTANCE.keyPressed(key, scanCode, modifiers)) info.cancel();
            } else if (action == 0) {
                if (ScreenOverlayHandler.INSTANCE.keyReleased(key, scanCode, modifiers)) info.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void head_charTyped_FancyMenu(long windowPointer, CharacterEvent event, CallbackInfo info) {
        if (windowPointer == WindowHandler.getWindowHandle()) {
            int codePoint = event.codepoint();
            int modifiers = 0;
            GlslRuntimeEventTracker.onCharTyped(codePoint, modifiers);
            if (Character.charCount(codePoint) == 1) {
                if (ScreenOverlayHandler.INSTANCE.charTyped((char)codePoint, modifiers)) info.cancel();
            } else {
                boolean cancel = false;
                for (char c : Character.toChars(codePoint)) {
                    if (ScreenOverlayHandler.INSTANCE.charTyped(c, modifiers)) cancel = true;
                }
                if (cancel) info.cancel();
            }
        }
    }

}
