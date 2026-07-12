package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.input.InputUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow {

    @Shadow @Final private long window;

    /**
     * @reason Clear semantic modifier state on focus loss because no later release event is guaranteed after a remapped modifier was held while focus changed.
     */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_FancyMenu(long windowPointer, boolean focused, CallbackInfo info) {
        if (windowPointer == this.window) InputUtils.onWindowFocusChanged(focused);
    }

}
