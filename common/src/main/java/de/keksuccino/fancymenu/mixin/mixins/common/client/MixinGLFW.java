package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.cursor.GlfwCursorTracker;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GLFW.class, remap = false)
public class MixinGLFW {

    @Inject(method = "glfwSetCursor", at = @At("HEAD"))
    private static void on_glfwSetCursor_FancyMenu(long window, long cursor, CallbackInfo ci) {
        GlfwCursorTracker.onGlfwSetCursor(window, cursor);
    }

    @Inject(method = "glfwCreateStandardCursor", at = @At("RETURN"))
    private static void after_glfwCreateStandardCursor_FancyMenu(int shape, CallbackInfoReturnable<Long> cir) {
        GlfwCursorTracker.onGlfwCreateStandardCursor(shape, cir.getReturnValue());
    }

    @Inject(method = "glfwDestroyCursor", at = @At("HEAD"))
    private static void on_glfwDestroyCursor_FancyMenu(long cursor, CallbackInfo ci) {
        GlfwCursorTracker.onGlfwDestroyCursor(cursor);
    }

}

