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
     * @reason Clear the semantic modifier cache when focus is lost because GLFW cannot synthesize releases for remapped physical modifier identities it did not track as pressed.
     */
    @Inject(method = "onFocus", at = @At("HEAD"))
    private void before_onFocus_FancyMenu(long window, boolean hasFocus, CallbackInfo info) {
        if ((window == this.window) && !hasFocus) InputUtils.resetActiveModifiers();
    }

}
