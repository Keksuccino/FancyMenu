package de.keksuccino.fancymenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs;
import org.jetbrains.annotations.NotNull;

/**
 * The hardware of the client, like the game's own debug screen shows it. Only call these on the render thread.
 */
public final class SystemSpecsUtils {

    private SystemSpecsUtils() {
    }

    @NotNull
    public static String getCpuInfo() {
        return DebugEntrySystemSpecs.getCpuInfo();
    }

    @NotNull
    public static String getGpuRenderer() {
        return RenderSystem.getDevice().getDeviceInfo().name();
    }

    /**
     * @return The version of the graphics driver, like the OpenGL version string.
     */
    @NotNull
    public static String getGpuDriverInfo() {
        return RenderSystem.getDevice().getDeviceInfo().driverInfo();
    }

}
