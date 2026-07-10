package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
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
    private void before_onFocus_FancyMenu(long window, boolean focused, CallbackInfo info) {
        if ((window == this.window) && !focused) {
            InputUtils.resetActiveModifiers();
            ScreenOverlayHandler.INSTANCE.cancelMouseCaptures();
        }
    }

}
