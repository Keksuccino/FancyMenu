package de.keksuccino.fancymenu.util.watermedia.vulkan;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reflection-backed implementation of WaterMedia's optional VKContext API.
 * Keeping the API interface out of the static type graph lets Fabric compile and run FancyMenu without WaterMedia.
 */
final class WatermediaVulkanContext implements InvocationHandler {

    private final VulkanDevice device;
    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    private final ConcurrentLinkedQueue<Runnable> retirements = new ConcurrentLinkedQueue<>();
    private final Object proxy;

    WatermediaVulkanContext(VulkanDevice device) {
        this.device = device;
        this.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        VK12.vkGetPhysicalDeviceMemoryProperties(device.vkDevice().getPhysicalDevice(), this.memoryProperties);
        try {
            Class<?> vkContextClass = Class.forName("org.watermedia.api.media.engines.vk.VKContext", false, WatermediaVulkanContext.class.getClassLoader());
            this.proxy = Proxy.newProxyInstance(vkContextClass.getClassLoader(), new Class<?>[]{vkContextClass}, this);
        } catch (ReflectiveOperationException ex) {
            this.memoryProperties.free();
            throw new IllegalStateException("Failed to create Watermedia's Vulkan context bridge", ex);
        }
    }

    VulkanDevice device() {
        return this.device;
    }

    Object proxy() {
        return this.proxy;
    }

    void beginDeviceClose() {
        this.drainRetirements();
    }

    void finishDeviceClose() {
        this.memoryProperties.free();
    }

    void drainRetirements() {
        Runnable retirement;
        while ((retirement = this.retirements.poll()) != null) {
            this.device.createCommandEncoder().queueForDestroy(retirement::run);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "vkInstance" -> this.device.instance().vkInstance();
            case "physicalDevice" -> this.device.vkDevice().getPhysicalDevice();
            case "vkDevice" -> this.device.vkDevice();
            case "queue" -> this.device.graphicsQueue().vkQueue();
            case "queueFamily" -> this.device.graphicsQueue().queueFamilyIndex();
            case "queueLock" -> this.device.graphicsQueue().vkQueue();
            case "memoryProperties" -> this.memoryProperties;
            case "hostImportSupported", "ycbcrSampler" -> false;
            case "minImportedHostPointerAlignment" -> 0L;
            case "retire" -> {
                this.retirements.add((Runnable)args[0]);
                yield null;
            }
            case "toString" -> "FancyMenu Watermedia VKContext";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unsupported Watermedia VKContext method: " + method);
        };
    }

}
