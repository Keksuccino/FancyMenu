package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.GLX;
import de.keksuccino.fancymenu.util.window.OffscreenTestClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.LongSupplier;

@Mixin(GLX.class)
public class MixinGLX {

    /**
     * @reason Prepares GLFW for the dev-only off-screen test client mode (see OffscreenTestClient).
     * Mojang ships GLX without obfuscation, so its methods have no mappings and must not get remapped.
     */
    @Inject(method = "_initGlfw", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwInit()Z"), remap = false)
    private static void before_glfwInit_FancyMenu(CallbackInfoReturnable<LongSupplier> info) {
        OffscreenTestClient.prepareBackendInit();
    }

}
