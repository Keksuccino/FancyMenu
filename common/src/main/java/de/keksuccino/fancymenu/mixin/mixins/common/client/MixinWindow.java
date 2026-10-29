package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow implements FancyWindow {

    @Shadow @Final private long handle;

    @Unique private double preciseScale_FancyMenu = -1;

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    private void void_before_setGuiScale_FancyMenu(int scale, CallbackInfo info) {
        // Reset precise scale when setting scale via Vanilla method
        this.preciseScale_FancyMenu = -1;
    }

    /** @reason Clear cached modifier and captured overlay input when focus is lost because GLFW may not deliver matching release events while unfocused. */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_FancyMenu(long handle, boolean focused, CallbackInfo info) {
        if ((handle == this.handle) && !focused) {
            InputUtils.resetActiveModifiers();
            ScreenOverlayHandler.INSTANCE.cancelMouseCaptures();
        }
    }

    // Dev-only off-screen test client mode (see OffscreenTestClient and TESTING_GUIDELINES.md)

    /** @reason Creates the game window hidden and unfocused in the off-screen test client mode. */
    @WrapOperation(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private static long wrap_glfwCreateWindow_FancyMenu(int width, int height, CharSequence title, long monitor, long share, Operation<Long> original) {
        OffscreenTestClient.prepareWindowCreation();
        return original.call(width, height, title, monitor, share);
    }

    /** @reason Keeps off-screen test clients windowed, because GLFW always shows fullscreen windows. */
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/DisplayData;isFullscreen()Z"))
    private boolean modify_isFullscreen_FancyMenu(boolean fullscreen) {
        return fullscreen && !OffscreenTestClient.isEnabled();
    }

    /** @reason Keeps off-screen test clients windowed, because GLFW always shows fullscreen windows. */
    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
    private void cancel_toggleFullScreen_FancyMenu(CallbackInfo info) {
        if (OffscreenTestClient.isEnabled()) info.cancel();
    }

    @Shadow public abstract int getGuiScale();

    @Unique
    @Override
    public double getPreciseGuiScale_FancyMenu() {
        if (this.preciseScale_FancyMenu <= 0) return this.getGuiScale();
        return this.preciseScale_FancyMenu;
    }

    @Unique
    @Override
    public void setPreciseGuiScale_FancyMenu(double scale) {
        this.preciseScale_FancyMenu = scale;
    }

}
