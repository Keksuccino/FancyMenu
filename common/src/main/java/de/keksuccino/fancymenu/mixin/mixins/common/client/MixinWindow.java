package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow {

    @Shadow @Final private long window;

    /** @reason Clear cached modifier and captured overlay input when focus is lost because GLFW may not deliver matching release events while unfocused. */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_FancyMenu(long windowPointer, boolean focused, CallbackInfo info) {
        if (windowPointer == this.window) {
            InputUtils.onWindowFocusChanged(focused);
            if (!focused) ScreenOverlayHandler.INSTANCE.cancelMouseCaptures();
        }
    }

    // Dev-only off-screen test client mode (see OffscreenTestClient and TESTING_GUIDELINES.md)

    /** @reason Creates the game window hidden and unfocused in the off-screen test client mode. */
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", remap = false))
    private void wrap_glfwDefaultWindowHints_FancyMenu(Operation<Void> original) {
        original.call();
        OffscreenTestClient.prepareWindowCreation();
    }

    /** @reason Keeps off-screen test clients windowed, because GLFW always shows fullscreen windows. */
    @ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/DisplayData;isFullscreen:Z"))
    private boolean modify_isFullscreen_FancyMenu(boolean fullscreen) {
        return fullscreen && !OffscreenTestClient.isEnabled();
    }

    /** @reason Keeps off-screen test clients windowed, because GLFW always shows fullscreen windows. */
    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
    private void cancel_toggleFullScreen_FancyMenu(CallbackInfo info) {
        if (OffscreenTestClient.isEnabled()) info.cancel();
    }

}
