package de.keksuccino.fancymenu.util.watermedia.vulkan;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import org.jetbrains.annotations.Nullable;

/**
 * Publishes the Vulkan device after the optional WaterMedia context mixin has attached to it.
 * The context remains typed as Object so loading FancyMenu without WaterMedia never resolves optional API classes.
 */
public final class WatermediaVulkanInterop {

    @Nullable private static volatile WatermediaVulkanContext bridge;

    private WatermediaVulkanInterop() {}

    public static void register(VulkanDevice vulkanDevice) {
        bridge = new WatermediaVulkanContext(vulkanDevice);
    }

    public static void beginDeviceClose(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current == null || current.device() != vulkanDevice) return;
        current.beginDeviceClose();
    }

    public static void finishDeviceClose(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current == null || current.device() != vulkanDevice) return;
        bridge = null;
        current.finishDeviceClose();
    }

    @Nullable
    public static VulkanDevice device() {
        WatermediaVulkanContext current = bridge;
        return current != null ? current.device() : null;
    }

    @Nullable
    public static Object context() {
        WatermediaVulkanContext current = bridge;
        return current != null ? current.proxy() : null;
    }

    public static void drainRetirements(VulkanDevice vulkanDevice) {
        WatermediaVulkanContext current = bridge;
        if (current != null && current.device() == vulkanDevice) current.drainRetirements();
    }

}
