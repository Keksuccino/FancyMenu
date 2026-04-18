package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class AfmaFrameNormalizer {

    @Nullable
    protected static volatile Boolean stbAvailable;

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file) throws IOException {
        return this.loadFrame(file, null);
    }

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, @Nullable AfmaFastPixelBufferPool pixelBufferPool) throws IOException {
        Objects.requireNonNull(file);
        DecodedImage decodedImage = this.decodeFrame(file);
        int[] pixels = AfmaPixelFrameHelper.allocatePixels(decodedImage.width(), decodedImage.height(), pixelBufferPool);
        System.arraycopy(decodedImage.pixels(), 0, pixels, 0, decodedImage.pixels().length);
        return new AfmaPixelFrame(decodedImage.width(), decodedImage.height(), pixels, pixelBufferPool);
    }

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, int expectedWidth, int expectedHeight) throws IOException {
        return this.loadFrame(file, expectedWidth, expectedHeight, null);
    }

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file, int expectedWidth, int expectedHeight,
                                    @Nullable AfmaFastPixelBufferPool pixelBufferPool) throws IOException {
        int[] pixels = AfmaPixelFrameHelper.allocatePixels(expectedWidth, expectedHeight, pixelBufferPool);
        try {
            this.loadFrameInto(file, expectedWidth, expectedHeight, pixels);
            return new AfmaPixelFrame(expectedWidth, expectedHeight, pixels, pixelBufferPool);
        } catch (IOException | RuntimeException | Error throwable) {
            if (pixelBufferPool != null) {
                pixelBufferPool.releasePixels(pixels);
            }
            throw throwable;
        }
    }

    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height) {
        return this.extractPatch(image, x, y, width, height, null);
    }

    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height,
                                       @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        return AfmaPixelFrameHelper.crop(image, x, y, width, height, pixelBufferPool);
    }

    public void loadFrameInto(@NotNull File file, int expectedWidth, int expectedHeight, @NotNull int[] targetPixels) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(targetPixels);
        int expectedPixelCount = AfmaPixelFrameHelper.pixelCount(expectedWidth, expectedHeight);
        if (targetPixels.length < expectedPixelCount) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the expected frame size");
        }
        DecodedImage decodedImage = this.decodeFrame(file);
        if ((decodedImage.width() != expectedWidth) || (decodedImage.height() != expectedHeight)) {
            throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + file.getAbsolutePath());
        }
        System.arraycopy(decodedImage.pixels(), 0, targetPixels, 0, expectedPixelCount);
    }

    @NotNull
    protected DecodedImage decodeFrame(@NotNull File file) throws IOException {
        if (isStbAvailable()) {
            return this.decodeFrameWithStb(file);
        }
        return this.decodeFrameWithImageIo(file);
    }

    protected boolean isStbAvailable() {
        Boolean cached = stbAvailable;
        if (cached != null) {
            return cached;
        }
        synchronized (AfmaFrameNormalizer.class) {
            cached = stbAvailable;
            if (cached != null) {
                return cached;
            }
            try {
                Class.forName("org.lwjgl.stb.STBImage");
                Class.forName("org.lwjgl.system.MemoryStack");
                stbAvailable = Boolean.TRUE;
            } catch (Throwable ignored) {
                stbAvailable = Boolean.FALSE;
            }
            return Objects.requireNonNull(stbAvailable);
        }
    }

    @NotNull
    protected DecodedImage decodeFrameWithStb(@NotNull File file) throws IOException {
        ByteBuffer decodedBuffer = null;
        AutoCloseable memoryStack = null;
        try {
            Class<?> memoryStackClass = Class.forName("org.lwjgl.system.MemoryStack");
            memoryStack = (AutoCloseable) memoryStackClass.getMethod("stackPush").invoke(null);
            Method mallocInt = memoryStackClass.getMethod("mallocInt", int.class);
            IntBuffer widthBuffer = (IntBuffer) mallocInt.invoke(memoryStack, 1);
            IntBuffer heightBuffer = (IntBuffer) mallocInt.invoke(memoryStack, 1);
            IntBuffer componentBuffer = (IntBuffer) mallocInt.invoke(memoryStack, 1);

            Class<?> stbImageClass = Class.forName("org.lwjgl.stb.STBImage");
            decodedBuffer = (ByteBuffer) stbImageClass
                    .getMethod("stbi_load", String.class, IntBuffer.class, IntBuffer.class, IntBuffer.class, int.class)
                    .invoke(null, file.getAbsolutePath(), widthBuffer, heightBuffer, componentBuffer, 4);
            if (decodedBuffer == null) {
                String failureReason = (String) stbImageClass.getMethod("stbi_failure_reason").invoke(null);
                throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath() + " (" + failureReason + ")");
            }

            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            int[] pixels = new int[AfmaPixelFrameHelper.pixelCount(width, height)];
            this.decodeFrameToPixels(decodedBuffer, width, height, pixels, file);
            return new DecodedImage(width, height, pixels);
        } catch (InvocationTargetException ex) {
            Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
            if (cause instanceof IOException ioEx) {
                throw ioEx;
            }
            throw new IOException("Failed to decode AFMA source frame via STB: " + file.getAbsolutePath(), cause);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            throw new IOException("Failed to initialize AFMA STB frame decoding for " + file.getAbsolutePath(), ex);
        } finally {
            if (decodedBuffer != null) {
                try {
                    Class<?> stbImageClass = Class.forName("org.lwjgl.stb.STBImage");
                    stbImageClass.getMethod("stbi_image_free", ByteBuffer.class).invoke(null, decodedBuffer);
                } catch (Throwable ignored) {
                }
            }
            if (memoryStack != null) {
                try {
                    memoryStack.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @NotNull
    protected DecodedImage decodeFrameWithImageIo(@NotNull File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath());
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        return new DecodedImage(width, height, pixels);
    }

    protected void decodeFrameToPixels(@NotNull ByteBuffer decodedBuffer, int width, int height,
                                       @NotNull int[] targetPixels, @NotNull File sourceFile) throws IOException {
        int pixelCount = AfmaPixelFrameHelper.pixelCount(width, height);
        if (targetPixels.length < pixelCount) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the decoded frame");
        }

        IntBuffer rgbaPixels = decodedBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        if (rgbaPixels.remaining() < pixelCount) {
            throw new IOException("Decoded AFMA source frame is shorter than expected: " + sourceFile.getAbsolutePath());
        }

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            int rgbaColor = rgbaPixels.get(pixelIndex);
            targetPixels[pixelIndex] = (rgbaColor & 0xFF00FF00)
                    | ((rgbaColor & 0x00FF0000) >>> 16)
                    | ((rgbaColor & 0x000000FF) << 16);
        }
    }

    protected record DecodedImage(int width, int height, @NotNull int[] pixels) {
    }

}
