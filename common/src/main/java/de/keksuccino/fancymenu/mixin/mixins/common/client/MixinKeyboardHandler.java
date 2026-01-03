package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.Util;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    /**
     * @reason Fire FancyMenu's key listeners after vanilla processing so they run both in menus and during gameplay.
     */
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void triggerKeyListeners_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (windowPointer != Minecraft.getInstance().getWindow().getWindow()) {
            return;
        }

        if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_KEY_RELEASED.handleKeyReleased(key, scanCode, modifiers);
        } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            Listeners.ON_KEY_PRESSED.handleKeyPressed(key, scanCode, modifiers);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void head_keyPress_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            if (action == 1 || action == 2) {
                if (PiPWindowHandler.keyPressed(key, scanCode, modifiers)) info.cancel();
            } else if (action == 0) {
                if (PiPWindowHandler.keyReleased(key, scanCode, modifiers)) info.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void head_charTyped_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            if (Character.charCount(codePoint) == 1) {
                if (PiPWindowHandler.charTyped((char)codePoint, modifiers)) info.cancel();
            } else {
                boolean cancel = false;
                for (char c : Character.toChars(codePoint)) {
                    if (PiPWindowHandler.charTyped(c, modifiers)) cancel = true;
                }
                if (cancel) info.cancel();
            }
        }
    }

}