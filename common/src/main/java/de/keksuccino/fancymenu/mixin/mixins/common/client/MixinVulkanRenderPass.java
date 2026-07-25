package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import de.keksuccino.fancymenu.util.watermedia.vulkan.WatermediaVulkanTextureView;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public class MixinVulkanRenderPass {

    @Unique private int sampledImageLayout_FancyMenu = VK12.VK_IMAGE_LAYOUT_GENERAL;

    /** @reason Remember whether the descriptor currently being assembled borrows WaterMedia's shader-read-only image view. */
    @WrapOperation(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vulkan/VulkanGpuTextureView;vkImageView()J"))
    private long wrap_imageView_FancyMenu(VulkanGpuTextureView textureView, Operation<Long> original) {
        if (textureView instanceof WatermediaVulkanTextureView watermediaView && watermediaView.hasExternalImageView()) {
            this.sampledImageLayout_FancyMenu = VK12.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        } else {
            this.sampledImageLayout_FancyMenu = VK12.VK_IMAGE_LAYOUT_GENERAL;
        }
        return original.call(textureView);
    }

    /**
     * @reason Minecraft emits imageView() immediately before imageLayout() for each sampled descriptor. WaterMedia
     * leaves its output in SHADER_READ_ONLY_OPTIMAL, so describing that borrowed view as Minecraft's normal GENERAL
     * layout would violate Vulkan's descriptor/image-layout contract.
     */
    @WrapOperation(method = "pushDescriptors", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkDescriptorImageInfo$Buffer;imageLayout(I)Lorg/lwjgl/vulkan/VkDescriptorImageInfo$Buffer;"))
    private VkDescriptorImageInfo.Buffer wrap_imageLayout_FancyMenu(VkDescriptorImageInfo.Buffer imageInfo, int imageLayout, Operation<VkDescriptorImageInfo.Buffer> original) {
        try {
            return original.call(imageInfo, this.sampledImageLayout_FancyMenu);
        } finally {
            this.sampledImageLayout_FancyMenu = VK12.VK_IMAGE_LAYOUT_GENERAL;
        }
    }

}
