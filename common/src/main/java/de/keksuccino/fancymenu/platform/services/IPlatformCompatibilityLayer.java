package de.keksuccino.fancymenu.platform.services;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.network.chat.Component;
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

}
