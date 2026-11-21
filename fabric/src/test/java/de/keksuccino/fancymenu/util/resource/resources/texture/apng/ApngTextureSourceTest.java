package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngTextureSourceTest {

    private long now = 1_000_000_000L;
    private final ApngFrameStreamTest.ManualExecutor executor = new ApngFrameStreamTest.ManualExecutor(() -> this.now);

    @Test
    void staticImageClosesItsSourceOnceItsTextureShowsIt() throws IOException {
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end());
        HeadlessTexture texture = this.load(source);
        this.executor.runTasks();
        assertFalse(source.isClosed());

        ResourceLocation location = texture.getResourceLocation();

        assertNotNull(location);
        assertEquals(1, texture.uploads);
        assertTrue(source.isClosed());
        assertNull(texture.stream.get());
        assertSame(location, texture.getResourceLocation());
    }

    @Test
    void animationKeepsShowingItsFrameWhenItsSourceChangesWhileItPlays() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(file);
        HeadlessTexture texture = this.load(source);
        this.executor.runTasks();
        ResourceLocation location = texture.getResourceLocation();
        assertNotNull(location);

        // The third frame gets decoded only after the change
        source.data = ApngFrameStreamTest.changedFrameData(file, 2);
        this.executor.runTasks();

        assertSame(location, texture.getResourceLocation());
        assertFalse(texture.isLoadingFailed());
        assertTrue(texture.isLoadingCompleted());
        assertTrue(source.isClosed());
        assertNull(texture.stream.get());
        assertSame(location, texture.getResourceLocation());
    }

    @Test
    void animationWhoseSourceChangesBeforeItsFirstFrameFailsToLoad() throws IOException {
        byte[] file = ApngFrameStreamTest.fourFrames();
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(file);
        HeadlessTexture texture = this.load(source);
        source.data = ApngFrameStreamTest.changedFrameData(file, 0);
        this.executor.runTasks();

        assertNull(texture.getResourceLocation());
        assertTrue(texture.isLoadingFailed());
        assertEquals(0, texture.uploads);
        assertTrue(source.isClosed());
    }

    private HeadlessTexture load(ApngTestFiles.MemorySource source) throws IOException {
        ApngImage image = ApngReader.read(source, "test.png");
        HeadlessTexture texture = new HeadlessTexture();
        texture.applyStream(image, new ApngFrameStream(image, "test.png", this.executor, () -> this.now));
        return texture;
    }

    /**
     * The real texture, just without its GPU texture.
     */
    private static final class HeadlessTexture extends ApngTexture {

        private int uploads;

        @Override
        protected void ensureGpuTexture(int textureWidth, int textureHeight) {
            if (this.textureLocation == null) this.textureLocation = new ResourceLocation("fancymenu", "test/apng");
        }

        @Override
        protected void upload(@NotNull ApngFrameUpdate update) {
            this.uploads++;
        }

    }

}
