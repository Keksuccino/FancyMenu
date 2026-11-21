package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads images of every format through their real textures and checks what happens to their sources.
 */
class AnimatedImageTextureSourceTest {

    private long now = 1_000_000_000L;
    private final AnimatedImageTestSupport.ManualExecutor executor = new AnimatedImageTestSupport.ManualExecutor(() -> this.now);
    private final List<AnimatedImageTexture> textures = new ArrayList<>();

    @AfterEach
    void closeTextures() {
        for (AnimatedImageTexture texture : this.textures) TestTextures.closeAndCheckMemory(texture, this.executor);
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void validImageLoadsThroughTheTexture(TestTextures.Format format) {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(format.fourFrames());

        AnimatedImageTexture texture = this.load(format, source);

        assertTrue(texture.isReady());
        assertTrue(texture.isLoadingCompleted());
        assertFalse(texture.isLoadingFailed());
        assertEquals(4, texture.getWidth());
        assertEquals(4, texture.getHeight());
        assertTrue(texture.animated);
        assertNotNull(texture.stream.get());
        assertFalse(source.isClosed());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void invalidImageDataPublishesTheFailure(TestTextures.Format format) {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(new byte[] {1, 2, 3});

        AnimatedImageTexture texture = this.load(format, source);

        assertTrue(texture.loadingFailed.get());
        assertTrue(texture.isReady());
        assertTrue(texture.isLoadingFailed());
        assertFalse(texture.isLoadingCompleted());
        assertNull(texture.getResourceLocation());
        assertTrue(source.isClosed());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void closedTextureDoesNotLoadAnymore(TestTextures.Format format) {
        AnimatedImageTexture texture = this.create(format);
        texture.closed.set(true);
        AtomicBoolean opened = new AtomicBoolean();

        texture.populate(() -> {
            opened.set(true);
            return new AnimatedImageTestSupport.MemorySource(format.stillImage());
        }, "image");

        assertFalse(opened.get());
        assertFalse(texture.loadingFailed.get());
        assertFalse(texture.decoded.get());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void closedTextureClosesItsStreamWithoutCopyingIt(TestTextures.Format format) {
        AnimatedImageTexture texture = this.create(format);
        texture.closed.set(true);
        AtomicBoolean streamClosed = new AtomicBoolean();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]) {
            @Override
            public void close() {
                streamClosed.set(true);
            }
        };

        texture.populate(in, "image");

        assertTrue(streamClosed.get());
        assertEquals(10, in.available());
        assertFalse(texture.decoded.get());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void staticImageClosesItsSourceOnceItsTextureShowsIt(TestTextures.Format format) {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(format.stillImage());
        AnimatedImageTexture texture = this.load(format, source);
        this.executor.runTasks();
        assertFalse(source.isClosed());

        ResourceLocation location = texture.getResourceLocation();

        assertNotNull(location);
        assertEquals(1, TestTextures.uploads(texture));
        assertTrue(source.isClosed());
        assertNull(texture.stream.get());
        assertSame(location, texture.getResourceLocation());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void animationKeepsShowingItsFrameWhenItsSourceChangesWhileItPlays(TestTextures.Format format) throws Exception {
        byte[] file = format.fourFrames();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        AnimatedImageTexture texture = this.load(format, source);
        this.executor.runTasks();
        ResourceLocation location = texture.getResourceLocation();
        assertNotNull(location);

        // Frames of 100 ms make the stream decode only two frames ahead, so the third frame gets decoded after the change
        source.data = format.changeFrameData(file, 2);
        this.executor.runTasks();

        assertSame(location, texture.getResourceLocation());
        assertFalse(texture.isLoadingFailed());
        assertTrue(texture.isLoadingCompleted());
        // The animation can't play any further, so things waiting for it to finish don't wait forever
        assertFalse(texture.isPlaying());
        assertTrue(source.isClosed());
        assertNull(texture.stream.get());
        assertSame(location, texture.getResourceLocation());
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void animationWhoseSourceChangesBeforeItsFirstFrameFailsToLoad(TestTextures.Format format) throws Exception {
        byte[] file = format.fourFrames();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        AnimatedImageTexture texture = this.load(format, source);
        source.data = format.changeFrameData(file, 0);
        this.executor.runTasks();

        assertNull(texture.getResourceLocation());
        assertTrue(texture.isLoadingFailed());
        assertEquals(0, TestTextures.uploads(texture));
        assertTrue(source.isClosed());
    }

    @NotNull
    private AnimatedImageTexture create(@NotNull TestTextures.Format format) {
        AnimatedImageTexture texture = format.createTexture(this.executor, () -> this.now);
        this.textures.add(texture);
        return texture;
    }

    @NotNull
    private AnimatedImageTexture load(@NotNull TestTextures.Format format, @NotNull AnimatedImageTestSupport.MemorySource source) {
        AnimatedImageTexture texture = this.create(format);
        texture.populate(() -> source, "image");
        return texture;
    }

}
