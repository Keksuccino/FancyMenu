package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.io.File;

@Mixin(Screenshot.class)
public class MixinScreenshot {

    /**
     * Vanilla saves grabbed screenshots inside a synthetic lambda of grab() (_grab() in older versions) that runs on an IO thread.
     * Lambda names are compiler-generated and change between Minecraft versions, so this targets all methods
     * and relies on the injection point instead, because Screenshot only calls NativeImage#writeToFile when saving a grabbed screenshot.
     * The listener only gets notified after the file got written, since writeToFile throws when saving fails.
     */
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;writeToFile(Ljava/io/File;)V"))
    private static void wrap_writeToFile_FancyMenu(NativeImage image, File file, Operation<Void> original) {
        original.call(image, file);
        Listeners.ON_SCREENSHOT_TAKEN.onScreenshotSaved(file);
    }

}
