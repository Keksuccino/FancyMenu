package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;

final class PngTextureMetadataReader {

    private PngTextureMetadataReader() {
    }

    @NotNull
    static Dimensions read(@NotNull Resource resource) throws IOException {
        return read(resource::open, NativeImage::read);
    }

    /**
     * Owns both resources for the complete metadata probe. {@link NativeImage#read(InputStream)} currently also closes
     * its input, but keeping ownership here prevents that implementation detail from becoming part of our contract.
     */
    @NotNull
    static Dimensions read(@NotNull IoSupplier<InputStream> inputSupplier, @NotNull NativeImageDecoder imageDecoder) throws IOException {
        try (InputStream input = inputSupplier.get(); NativeImage image = imageDecoder.read(input)) {
            return new Dimensions(image.getWidth(), image.getHeight());
        }
    }

    @FunctionalInterface
    interface NativeImageDecoder {

        @NotNull
        NativeImage read(@NotNull InputStream input) throws IOException;

    }

    record Dimensions(int width, int height) {
    }

}
