package de.keksuccino.fancymenu.platform.services;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IPlatformCompatibilityLayer {

    /**
     * Returns the left-aligned title-screen branding block in visual top-to-bottom order.
     * Right-aligned status lines rendered above the copyright notice are not part of this block.
     */
    List<Component> getTitleScreenBrandingLines();

    /**
     * Returns the backend texture behind loader-side texture wrappers.
     * NeoForge's Blaze3D validation layer wraps every {@link GpuTexture}, so code that needs the backend object (e.g. an OpenGL texture ID) has to unwrap it first.
     */
    @Nullable
    GpuTexture getBackendTexture(@Nullable GpuTexture texture);

    /**
     * Returns the backend device behind loader-side device wrappers.
     * NeoForge's Blaze3D validation layer only accepts textures it created itself, so views of textures that wrap externally owned backend objects (e.g. another mod's OpenGL texture) have to be created on the backend device.
     */
    @NotNull
    GpuDevice getBackendDevice(@NotNull GpuDevice device);

}
