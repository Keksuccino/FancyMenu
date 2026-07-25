package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.mojang.blaze3d.vulkan.VulkanQueue$Submission")
public class MixinVulkanQueueSubmission {

    /** @reason Serialize Minecraft and WaterMedia submissions that share Minecraft's graphics VkQueue. */
    @WrapOperation(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/KHRSynchronization2;vkQueueSubmit2KHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkSubmitInfo2$Buffer;J)I"))
    private int wrap_submit_FancyMenu(VkQueue queue, VkSubmitInfo2.Buffer submits, long fence, Operation<Integer> original) {
        synchronized (queue) {
            return original.call(queue, submits, fence);
        }
    }

}
