package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.input.ScreenKeyEventDispatcher;
import de.keksuccino.fancymenu.util.input.Utf16CodeUnitDispatcher;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The reduced priority keeps FancyMenu's screen-call wrappers inside Fabric API's default-priority wrappers. This is
 * important because a Fabric allow-event cancellation must skip FancyMenu's post-screen event along with the actual
 * {@link Screen} call.
 */
@Mixin(value = KeyboardHandler.class, priority = 900)
public abstract class MixinKeyboardHandler {

    /**
     * @reason Fire FancyMenu's screen-key event after 1.19.2's loader-patched screen handling task completes.
     *
     * The actual screen calls live in loader-specific synthetic lambdas in this Minecraft version. Wrapping their
     * stable {@code Screen.wrapScreenError} owner keeps the injector portable while preserving Rinku cancellation: if
     * an earlier hook cancels {@code keyPress}, Vanilla never reaches this operation and no event is dispatched.
     */
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void wrap_screenKeyTask_in_keyPress_FancyMenu(Runnable screenTask, String errorDescription, String screenName, Operation<Void> operation, long windowPointer, int key, int scanCode, int action, int modifiers) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            operation.call(screenTask, errorDescription, screenName);
            return;
        }

        Runnable dispatchedTask = () -> ScreenKeyEventDispatcher.dispatchAfterScreenTask(windowPointer, action, screen, key, scanCode, modifiers, screenTask);
        operation.call(dispatchedTask, errorDescription, screenName);
    }

    /**
     * @reason Fire FancyMenu's key listeners after vanilla processing so they run both in menus and during gameplay.
     */
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void triggerKeyListeners_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (windowPointer != Minecraft.getInstance().getWindow().getWindow()) {
            return;
        }

        if (action == GLFW.GLFW_RELEASE) {
            if (Listeners.ON_KEY_RELEASED.hasInstancesListening()) Listeners.ON_KEY_RELEASED.handleKeyReleased(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyReleased(key, scanCode, modifiers);
        } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (Listeners.ON_KEY_PRESSED.hasInstancesListening()) Listeners.ON_KEY_PRESSED.handleKeyPressed(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyPressed(key, scanCode, modifiers, action == GLFW.GLFW_REPEAT);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void head_keyPress_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            InputUtils.updateActiveModifiers(modifiers);
            if (action == 1 || action == 2) {
                if (ScreenOverlayHandler.INSTANCE.keyPressed(key, scanCode, modifiers)) info.cancel();
            } else if (action == 0) {
                if (ScreenOverlayHandler.INSTANCE.keyReleased(key, scanCode, modifiers)) info.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void head_charTyped_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            InputUtils.updateActiveModifiers(modifiers);
            GlslRuntimeEventTracker.onCharTyped(codePoint, modifiers);
            if (Utf16CodeUnitDispatcher.dispatch(codePoint, modifiers, ScreenOverlayHandler.INSTANCE::charTyped)) info.cancel();
        }
    }

}
