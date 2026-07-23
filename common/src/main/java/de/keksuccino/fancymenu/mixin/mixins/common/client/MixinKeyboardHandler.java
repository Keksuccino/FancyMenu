package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.input.ScreenKeyEventDispatcher;
import de.keksuccino.fancymenu.util.input.Utf16CodeUnitDispatcher;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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
     * @reason Fire FancyMenu's screen-key event after the exact screen call even when the screen consumes the key.
     *
     * MCEF forwarding can cancel {@code KeyboardHandler.keyPress} before Vanilla calls the screen. In that case this
     * post-call hook must not run, preserving the established behavior on both loaders.
     */
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean wrap_keyPressed_in_keyPress_FancyMenu(Screen screen, KeyEvent event, Operation<Boolean> operation, long windowPointer, int action) {
        return ScreenKeyEventDispatcher.dispatchAfterScreenCall(windowPointer, action, screen, event, () -> operation.call(screen, event));
    }

    /**
     * @reason Fire FancyMenu's screen-key event after the exact screen call even when the screen consumes the key.
     *
     * See {@code wrap_keyPressed_in_keyPress_FancyMenu} for the intentional MCEF cancellation ordering.
     */
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean wrap_keyReleased_in_keyPress_FancyMenu(Screen screen, KeyEvent event, Operation<Boolean> operation, long windowPointer, int action) {
        return ScreenKeyEventDispatcher.dispatchAfterScreenCall(windowPointer, action, screen, event, () -> operation.call(screen, event));
    }

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
            if (Listeners.ON_KEY_RELEASED.hasInstancesListening()) Listeners.ON_KEY_RELEASED.handleKeyReleased(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyReleased(key, scanCode, modifiers);
        } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (Listeners.ON_KEY_PRESSED.hasInstancesListening()) Listeners.ON_KEY_PRESSED.handleKeyPressed(key, scanCode, modifiers);
            GlslRuntimeEventTracker.onKeyPressed(key, scanCode, modifiers, action == GLFW.GLFW_REPEAT);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void head_keyPress_FancyMenu(long windowPointer, int action, KeyEvent event, CallbackInfo info) {
        if (windowPointer == WindowHandler.getWindowHandle()) {
            InputUtils.updateActiveModifiers(event.modifiers());
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
            int modifiers = event.modifiers();
            GlslRuntimeEventTracker.onCharTyped(codePoint, modifiers);
            if (Utf16CodeUnitDispatcher.dispatch(codePoint, modifiers, ScreenOverlayHandler.INSTANCE::charTyped)) info.cancel();
        }
    }

}
