package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.gif.GifFrame;
import de.keksuccino.fancymenu.util.resource.resources.texture.gif.GifTestFiles;
import de.keksuccino.fancymenu.util.resource.resources.texture.gif.GifTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plays APNG and GIF images through their real textures, with textures in memory instead of GPU textures and on a fake clock, and builds the same test images in both formats.
 */
final class TestTextures {

    /** RGB colors of the GIF test images: red, green, blue, white. **/
    static final int[] GIF_COLORS = {255, 0, 0, 0, 255, 0, 0, 0, 255, 255, 255, 255};

    private TestTextures() {
    }

    /**
     * How often the texture uploaded pixels to its texture.
     */
    static int uploads(@NotNull AnimatedImageTexture texture) {
        return recorder(texture).uploads;
    }

    @NotNull
    static UploadRecorder recorder(@NotNull AnimatedImageTexture texture) {
        return ((Headless) texture).recorder();
    }

    /**
     * Closes the streams of the texture like closing the texture does, runs the tasks that free their memory and checks that no native memory is left.
     */
    static void closeAndCheckMemory(@NotNull AnimatedImageTexture texture, @NotNull AnimatedImageTestSupport.ManualExecutor executor) {
        AnimatedFrameStream active = texture.stream.get();
        AnimatedFrameStream playing = texture.playingStream;
        texture.releaseRenderResources();
        executor.runTasks();
        if (active != null) assertEquals(0L, active.getNativeMemoryBytes());
        if (playing != null) assertEquals(0L, playing.getNativeMemoryBytes());
    }

    /**
     * The formats with their textures and the same test images in each format.
     */
    enum Format {

        APNG {
            @Override
            @NotNull
            AnimatedImageTexture createTexture(@NotNull AnimatedImageTestSupport.ManualExecutor executor, @NotNull LongSupplier clock) {
                return new HeadlessApngTexture(executor, clock);
            }

            @Override
            @NotNull
            byte[] stillImage() {
                return ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 255)).end();
            }

            @Override
            @NotNull
            byte[] fourFrames() {
                return ApngTestFiles.fourFrames();
            }

            @Override
            @NotNull
            byte[] changeFrameData(@NotNull byte[] file, int frameIndex) throws IOException {
                return ApngTestFiles.changedFrameData(file, frameIndex);
            }
        },
        GIF {
            @Override
            @NotNull
            AnimatedImageTexture createTexture(@NotNull AnimatedImageTestSupport.ManualExecutor executor, @NotNull LongSupplier clock) {
                return new HeadlessGifTexture(executor, clock);
            }

            @Override
            @NotNull
            byte[] stillImage() {
                return GifTestFiles.gif(4, 4, GIF_COLORS).image(GifTestFiles.solidFrame(4, 4, 2)).end();
            }

            @Override
            @NotNull
            byte[] fourFrames() {
                GifTestFiles.Gif gif = GifTestFiles.gif(4, 4, GIF_COLORS).netscapeLoop(0);
                gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(4, 4, 0));
                gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 1));
                gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 2).at(2, 2));
                gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(1, 1, 3).at(1, 1));
                return gif.end();
            }

            @Override
            @NotNull
            byte[] changeFrameData(@NotNull byte[] file, int frameIndex) throws IOException {
                GifFrame frame = GifTestFiles.read(file).getFrame(frameIndex);
                byte[] changed = file.clone();
                changed[(int) (frame.dataPosition() + frame.dataLength() / 2)] ^= 0x10;
                return changed;
            }
        };

        @NotNull
        abstract AnimatedImageTexture createTexture(@NotNull AnimatedImageTestSupport.ManualExecutor executor, @NotNull LongSupplier clock);

        /**
         * A 4x4 image with a single frame.
         */
        @NotNull
        abstract byte[] stillImage();

        /**
         * Four 4x4 frames that loop forever, every frame shows for 100 ms.
         */
        @NotNull
        abstract byte[] fourFrames();

        /**
         * A copy of the file with one changed byte in the middle of the data of a frame, like an edit of the file.
         */
        @NotNull
        abstract byte[] changeFrameData(@NotNull byte[] file, int frameIndex) throws IOException;

    }

    /**
     * A texture that writes its uploads to pixels in memory instead of a GPU texture.
     */
    interface Headless {

        @NotNull
        UploadRecorder recorder();

    }

    /**
     * Applies uploads to pixels in memory, like the GPU texture would, and remembers what got uploaded.
     */
    static final class UploadRecorder {

        int uploads;
        int lastUploadedFrame = -1;
        @NotNull
        int[] pixels = new int[0];
        private int width;
        /** Runs whenever the texture notifies the listeners that a play finished. **/
        @Nullable
        Runnable onFinishedPlaying;

        void ensure(int textureWidth, int textureHeight) {
            if (this.pixels.length != textureWidth * textureHeight) this.pixels = new int[textureWidth * textureHeight];
            this.width = textureWidth;
        }

        void record(@NotNull AnimatedFrameUpdate update) {
            this.uploads++;
            this.lastUploadedFrame = update.getFrameIndex();
            IntBuffer source = Objects.requireNonNull(update.getPixels()).duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            int offset = 0;
            for (int i = 0; i < update.getRectangleCount(); i++) {
                for (int row = 0; row < update.getRectangleHeight(i); row++) {
                    for (int column = 0; column < update.getRectangleWidth(i); column++) {
                        this.pixels[(update.getRectangleY(i) + row) * this.width + update.getRectangleX(i) + column] = source.get(offset++);
                    }
                }
            }
        }

    }

    /**
     * The real APNG texture, decoding on the manual executor and playing on the fake clock, with its texture in memory.
     */
    static final class HeadlessApngTexture extends ApngTexture implements Headless {

        @NotNull
        private final AnimatedImageTestSupport.ManualExecutor executor;
        @NotNull
        private final LongSupplier clock;
        private final UploadRecorder recorder = new UploadRecorder();

        private HeadlessApngTexture(@NotNull AnimatedImageTestSupport.ManualExecutor executor, @NotNull LongSupplier clock) {
            this.executor = executor;
            this.clock = clock;
        }

        @Override
        protected void applyImage(@NotNull AnimatedImage image, @NotNull String name) {
            this.applyStream(image, new AnimatedFrameStream(image, name, this.executor, this.clock));
        }

        @Override
        protected long nanoTime() {
            return this.clock.getAsLong();
        }

        @Override
        protected void ensureGpuTexture(int textureWidth, int textureHeight) {
            this.recorder.ensure(textureWidth, textureHeight);
            if (this.textureLocation == null) this.textureLocation = new ResourceLocation("fancymenu", "test/apng");
        }

        @Override
        protected void upload(@NotNull AnimatedFrameUpdate update) {
            this.recorder.record(update);
        }

        @Override
        protected void releaseGpuTexture() {
            this.textureLocation = null;
        }

        @Override
        protected void notifyAnimatedTextureFinished(boolean willRestart) {
            super.notifyAnimatedTextureFinished(willRestart);
            if (this.recorder.onFinishedPlaying != null) this.recorder.onFinishedPlaying.run();
        }

        @Override
        @NotNull
        public UploadRecorder recorder() {
            return this.recorder;
        }

    }

    /**
     * The real GIF texture, decoding on the manual executor and playing on the fake clock, with its texture in memory.
     */
    static final class HeadlessGifTexture extends GifTexture implements Headless {

        @NotNull
        private final AnimatedImageTestSupport.ManualExecutor executor;
        @NotNull
        private final LongSupplier clock;
        private final UploadRecorder recorder = new UploadRecorder();

        private HeadlessGifTexture(@NotNull AnimatedImageTestSupport.ManualExecutor executor, @NotNull LongSupplier clock) {
            this.executor = executor;
            this.clock = clock;
        }

        @Override
        protected void applyImage(@NotNull AnimatedImage image, @NotNull String name) {
            this.applyStream(image, new AnimatedFrameStream(image, name, this.executor, this.clock));
        }

        @Override
        protected long nanoTime() {
            return this.clock.getAsLong();
        }

        @Override
        protected void ensureGpuTexture(int textureWidth, int textureHeight) {
            this.recorder.ensure(textureWidth, textureHeight);
            if (this.textureLocation == null) this.textureLocation = new ResourceLocation("fancymenu", "test/gif");
        }

        @Override
        protected void upload(@NotNull AnimatedFrameUpdate update) {
            this.recorder.record(update);
        }

        @Override
        protected void releaseGpuTexture() {
            this.textureLocation = null;
        }

        @Override
        protected void notifyAnimatedTextureFinished(boolean willRestart) {
            super.notifyAnimatedTextureFinished(willRestart);
            if (this.recorder.onFinishedPlaying != null) this.recorder.onFinishedPlaying.run();
        }

        @Override
        @NotNull
        public UploadRecorder recorder() {
            return this.recorder;
        }

    }

}
