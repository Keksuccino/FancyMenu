package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuSampler;

import javax.annotation.Nonnull;
import java.util.Objects;

final class GuiTextureSamplerUtil {

    private GuiTextureSamplerUtil() {
    }

    /**
     * Returns a clamp-to-edge variant of a texture's sampler without changing its filtering behavior.
     * Smooth image quads extend beyond their visible shape for anti-aliasing, so a repeating sampler would
     * otherwise expose pixels from the opposite texture edge in partially covered boundary fragments.
     */
    static GpuSampler clampToEdge(@Nonnull GpuSampler source) {
        Objects.requireNonNull(source);
        if (source.getAddressModeU() == AddressMode.CLAMP_TO_EDGE && source.getAddressModeV() == AddressMode.CLAMP_TO_EDGE) {
            return source;
        }
        return clampToEdge(source, RenderSystem.getSamplerCache());
    }

    static GpuSampler clampToEdge(@Nonnull GpuSampler source, @Nonnull SamplerCache samplerCache) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(samplerCache);
        // SamplerCache represents enabled mipmaps with an unrestricted (empty) max LOD and disabled mipmaps with LOD 0.
        boolean useMipmaps = source.getMaxLod().isEmpty();
        return samplerCache.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, source.getMinFilter(), source.getMagFilter(), useMipmaps);
    }
}
