package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import de.keksuccino.fancymenu.util.watermedia.vulkan.WatermediaVulkanInterop;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

@Mixin(VulkanDevice.class)
public class MixinVulkanDevice {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_construct_FancyMenu(ShaderSource defaultShaderSource, VulkanInstance instance, VulkanPhysicalDevice physicalDevice, Set<String> enabledDeviceExtensions, VkDevice vkDevice, long vma, CheckpointExtension checkpointExtension, CallbackInfo info) {
        WatermediaVulkanInterop.register((VulkanDevice)(Object)this);
    }

    /** @reason Publish device shutdown to the optional WaterMedia context before Minecraft destroys Vulkan resources. */
    @Inject(method = "close", at = @At("HEAD"))
    private void before_close_FancyMenu(CallbackInfo info) {
        WatermediaVulkanInterop.beginDeviceClose((VulkanDevice)(Object)this);
    }

    /** @reason Release the context-owned native memory snapshot after Minecraft has drained retired WaterMedia resources. */
    @Inject(method = "close", at = @At("RETURN"))
    private void after_close_FancyMenu(CallbackInfo info) {
        WatermediaVulkanInterop.finishDeviceClose((VulkanDevice)(Object)this);
    }

}
