package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

public class AfmaFrameNormalizer {

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file) throws IOException {
        Objects.requireNonNull(file);

        ByteBuffer decodedBuffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer componentBuffer = stack.mallocInt(1);
            decodedBuffer = STBImage.stbi_load(file.getAbsolutePath(), widthBuffer, heightBuffer, componentBuffer, 4);
            if (decodedBuffer == null) {
                throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath() + " (" + STBImage.stbi_failure_reason() + ")");
            }

            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            int[] pixels = new int[width * height];
            int byteOffset = 0;
            for (int i = 0; i < pixels.length; i++) {
                int red = decodedBuffer.get(byteOffset++) & 0xFF;
                int green = decodedBuffer.get(byteOffset++) & 0xFF;
                int blue = decodedBuffer.get(byteOffset++) & 0xFF;
                int alpha = decodedBuffer.get(byteOffset++) & 0xFF;
                pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
            return new AfmaPixelFrame(width, height, pixels);
        } finally {
            if (decodedBuffer != null) {
                STBImage.stbi_image_free(decodedBuffer);
            }
        }
    }

    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height) {
        return AfmaPixelFrameHelper.crop(image, x, y, width, height);
    }

}
