package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTextureSamplerUtilTest {

    @Test
    void returnsSamplerThatAlreadyClampsBothAxes() {
        StubSampler source = new StubSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.LINEAR, OptionalDouble.empty());

        assertSame(source, GuiTextureSamplerUtil.clampToEdge(source));
    }

    @Test
    void clampsBothAxesWhilePreservingFiltersAndEnabledMipmaps() {
        StubSampler source = new StubSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.LINEAR, OptionalDouble.empty());
        StubSampler replacement = new StubSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.LINEAR, OptionalDouble.empty());
        RecordingSamplerCache samplerCache = new RecordingSamplerCache(replacement);

        assertSame(replacement, GuiTextureSamplerUtil.clampToEdge(source, samplerCache));
        assertEquals(AddressMode.CLAMP_TO_EDGE, samplerCache.addressModeU);
        assertEquals(AddressMode.CLAMP_TO_EDGE, samplerCache.addressModeV);
        assertEquals(FilterMode.NEAREST, samplerCache.minFilter);
        assertEquals(FilterMode.LINEAR, samplerCache.magFilter);
        assertTrue(samplerCache.useMipmaps);
    }

    @Test
    void preservesDisabledMipmaps() {
        StubSampler source = new StubSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, OptionalDouble.of(0.0));
        RecordingSamplerCache samplerCache = new RecordingSamplerCache(source);

        GuiTextureSamplerUtil.clampToEdge(source, samplerCache);

        assertFalse(samplerCache.useMipmaps);
        assertEquals(FilterMode.LINEAR, samplerCache.minFilter);
        assertEquals(FilterMode.NEAREST, samplerCache.magFilter);
    }

    @Test
    void rejectsNullInputs() {
        StubSampler source = new StubSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, OptionalDouble.of(0.0));
        RecordingSamplerCache samplerCache = new RecordingSamplerCache(source);

        assertThrows(NullPointerException.class, () -> GuiTextureSamplerUtil.clampToEdge(null));
        assertThrows(NullPointerException.class, () -> GuiTextureSamplerUtil.clampToEdge(null, samplerCache));
        assertThrows(NullPointerException.class, () -> GuiTextureSamplerUtil.clampToEdge(source, null));
    }

    private static final class RecordingSamplerCache extends SamplerCache {

        private final GpuSampler result;
        private AddressMode addressModeU;
        private AddressMode addressModeV;
        private FilterMode minFilter;
        private FilterMode magFilter;
        private boolean useMipmaps;

        private RecordingSamplerCache(GpuSampler result) {
            this.result = result;
        }

        @Override
        public GpuSampler getSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
            this.addressModeU = addressModeU;
            this.addressModeV = addressModeV;
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.useMipmaps = useMipmaps;
            return this.result;
        }
    }

    private static final class StubSampler extends GpuSampler {

        private final AddressMode addressModeU;
        private final AddressMode addressModeV;
        private final FilterMode minFilter;
        private final FilterMode magFilter;
        private final OptionalDouble maxLod;

        private StubSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, OptionalDouble maxLod) {
            this.addressModeU = addressModeU;
            this.addressModeV = addressModeV;
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.maxLod = maxLod;
        }

        @Override
        public AddressMode getAddressModeU() {
            return this.addressModeU;
        }

        @Override
        public AddressMode getAddressModeV() {
            return this.addressModeV;
        }

        @Override
        public FilterMode getMinFilter() {
            return this.minFilter;
        }

        @Override
        public FilterMode getMagFilter() {
            return this.magFilter;
        }

        @Override
        public int getMaxAnisotropy() {
            return 1;
        }

        @Override
        public OptionalDouble getMaxLod() {
            return this.maxLod;
        }

        @Override
        public void close() {
        }
    }
}
