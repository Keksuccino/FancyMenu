package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow implements FancyWindow {

    @Unique private double preciseScale_FancyMenu = -1;

    @Shadow
    public abstract int getGuiScale();

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    private void void_before_setGuiScale_FancyMenu(int scale, CallbackInfo info) {
        // Reset precise scale when setting scale via Vanilla method
        this.preciseScale_FancyMenu = -1;
    }

    /** @reason Clear cached modifier and captured overlay input when focus is lost because SDL may not deliver matching release events while unfocused. */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_FancyMenu(boolean focused, CallbackInfo info) {
        if (!focused) {
            InputUtils.resetActiveModifiers();
            ScreenOverlayHandler.INSTANCE.cancelMouseCaptures();
        }
    }

    // Dev-only off-screen test client mode (see OffscreenTestClient and TESTING_GUIDELINES.md)

    /** @reason Creates the game window hidden in the off-screen test client mode. */
    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"), index = 3)
    private long modify_createWindow_FancyMenu(long flags) {
        return OffscreenTestClient.modifyWindowFlags(flags);
    }

    /** @reason Keeps off-screen test clients windowed, because SDL shows fullscreen windows. */
    // Targets all constructors, because the constructor without maximum window size only delegates to the other one
    @ModifyExpressionValue(method = "<init>*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/DisplayData;isFullscreen()Z"))
    private boolean modify_isFullscreen_FancyMenu(boolean fullscreen) {
        return fullscreen && !OffscreenTestClient.isEnabled();
    }

    /** @reason Keeps off-screen test clients windowed, because SDL shows fullscreen windows. */
    @Inject(method = "setFullscreen", at = @At("HEAD"), cancellable = true)
    private void cancel_setFullscreen_FancyMenu(boolean fullscreen, CallbackInfo info) {
        if (fullscreen && OffscreenTestClient.isEnabled()) info.cancel();
    }

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
