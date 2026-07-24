package de.keksuccino.fancymenu.util.resource.resources.texture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureManagerEntryTest {

    private static final Identifier FIRST_ID = Identifier.fromNamespaceAndPath("fancymenu", "dynamic/test_first");
    private static final Identifier SECOND_ID = Identifier.fromNamespaceAndPath("fancymenu", "dynamic/test_second");

    @Test
    void registeredEntryReleasesThroughTextureManagerExactlyOnce() {
        Fixture fixture = new Fixture(true);
        FakeImage image = new FakeImage();

        assertTrue(fixture.entry.adopt(image));
        assertEquals(FIRST_ID, fixture.entry.register(FIRST_ID));
        assertSame(image, fixture.entry.getImage());
        assertTrue(fixture.manager.textures.containsKey(FIRST_ID));

        fixture.entry.close();
        fixture.entry.close();

        assertFalse(fixture.manager.textures.containsKey(FIRST_ID));
        assertEquals(1, fixture.manager.releaseCalls.get());
        assertEquals(1, fixture.createdTexture.get().closeCalls.get());
        assertEquals(1, image.closeCalls.get());
        assertNull(fixture.entry.getIdentifier());
        assertNull(fixture.entry.getImage());
    }

    @Test
    void offThreadCloseRemainsOwnedUntilMainThreadFlush() {
        Fixture fixture = new Fixture(false);
        FakeImage image = new FakeImage();
        fixture.entry.adopt(image);
        fixture.entry.register(FIRST_ID);

        fixture.entry.close();

        assertTrue(fixture.manager.textures.containsKey(FIRST_ID));
        assertEquals(0, fixture.manager.releaseCalls.get());
        assertEquals(1, fixture.dispatcher.pendingReleaseCount());
        assertEquals(1, fixture.scheduledTasks.size());

        fixture.dispatcher.flush();

        assertFalse(fixture.manager.textures.containsKey(FIRST_ID));
        assertEquals(1, fixture.manager.releaseCalls.get());
        assertEquals(1, image.closeCalls.get());
        assertEquals(0, fixture.dispatcher.pendingReleaseCount());

        fixture.scheduledTasks.getFirst().run();
        fixture.dispatcher.flush();
        assertEquals(1, fixture.manager.releaseCalls.get());
        assertEquals(1, image.closeCalls.get());
    }

    @Test
    void offThreadReleaseBatchSchedulesOneDrainAndExecutesEveryReleaseOnce() {
        AtomicBoolean mainThread = new AtomicBoolean(false);
        AtomicInteger releaseCalls = new AtomicInteger();
        List<Runnable> scheduledTasks = new ArrayList<>();
        TextureManagerReleaseDispatcher.Dispatcher dispatcher = new TextureManagerReleaseDispatcher.Dispatcher(mainThread::get, scheduledTasks::add);

        for (int i = 0; i < 256; i++) dispatcher.dispatch(releaseCalls::incrementAndGet);

        assertEquals(256, dispatcher.pendingReleaseCount());
        assertEquals(1, scheduledTasks.size());

        scheduledTasks.getFirst().run();

        assertEquals(256, releaseCalls.get());
        assertEquals(0, dispatcher.pendingReleaseCount());
        assertEquals(1, scheduledTasks.size());

        scheduledTasks.getFirst().run();
        dispatcher.flush();
        assertEquals(256, releaseCalls.get());
    }

    @Test
    void unregisteredAndLateImagesCloseWithoutTextureManagerMutation() {
        Fixture fixture = new Fixture(false);
        FakeImage pendingImage = new FakeImage();
        FakeImage lateImage = new FakeImage();
        fixture.entry.adopt(pendingImage);

        fixture.entry.close();

        assertEquals(1, pendingImage.closeCalls.get());
        assertFalse(fixture.entry.adopt(lateImage));
        assertEquals(1, lateImage.closeCalls.get());
        assertEquals(0, fixture.manager.releaseCalls.get());
        assertEquals(0, fixture.dispatcher.pendingReleaseCount());
        assertTrue(fixture.scheduledTasks.isEmpty());
    }

    @Test
    void failedRegistrationClosesTransferredTextureWithoutRelease() {
        Fixture fixture = new Fixture(true);
        FakeImage image = new FakeImage();
        fixture.manager.failRegistration.set(true);
        fixture.entry.adopt(image);

        assertThrows(IllegalStateException.class, () -> fixture.entry.register(FIRST_ID));

        assertEquals(1, fixture.createdTexture.get().closeCalls.get());
        assertEquals(1, image.closeCalls.get());
        assertEquals(0, fixture.manager.releaseCalls.get());
        assertNull(fixture.entry.register(FIRST_ID));
        fixture.entry.close();
        assertEquals(1, image.closeCalls.get());
    }

    @Test
    void failedTextureConstructionClosesDecodedImage() {
        AtomicInteger releaseCalls = new AtomicInteger();
        FakeImage image = new FakeImage();
        TextureManagerEntry<FakeImage, FakeTexture> entry = new TextureManagerEntry<>((identifier, decodedImage) -> {
            throw new IllegalStateException("expected test failure");
        }, (identifier, texture) -> {}, identifier -> releaseCalls.incrementAndGet(), FakeTexture::image, Runnable::run);
        entry.adopt(image);

        assertThrows(IllegalStateException.class, () -> entry.register(FIRST_ID));

        assertEquals(1, image.closeCalls.get());
        assertEquals(0, releaseCalls.get());
    }

    @Test
    void duplicateAdoptionClosesRejectedImage() {
        Fixture fixture = new Fixture(true);
        FakeImage first = new FakeImage();
        FakeImage duplicate = new FakeImage();
        fixture.entry.adopt(first);

        assertThrows(IllegalStateException.class, () -> fixture.entry.adopt(duplicate));

        assertEquals(0, first.closeCalls.get());
        assertEquals(1, duplicate.closeCalls.get());
        fixture.entry.close();
        assertEquals(1, first.closeCalls.get());
    }

    @Test
    void closingReplacedEntryDoesNotReleaseReplacementIdentifier() {
        Fixture first = new Fixture(true);
        Fixture second = new Fixture(true, first.manager);
        first.entry.adopt(new FakeImage());
        second.entry.adopt(new FakeImage());
        first.entry.register(FIRST_ID);
        second.entry.register(SECOND_ID);

        first.entry.close();

        assertFalse(first.manager.textures.containsKey(FIRST_ID));
        assertTrue(first.manager.textures.containsKey(SECOND_ID));
        assertEquals(1, first.manager.releaseCalls.get());

        second.entry.close();
        assertTrue(first.manager.textures.isEmpty());
        assertEquals(2, first.manager.releaseCalls.get());
    }

    @Test
    void closeAndRegistrationRaceHasOneDeterministicOwner() throws Exception {
        FakeManager manager = new FakeManager();
        CountDownLatch factoryEntered = new CountDownLatch(1);
        CountDownLatch allowFactory = new CountDownLatch(1);
        TextureManagerEntry<FakeImage, FakeTexture> entry = new TextureManagerEntry<>((identifier, image) -> {
            factoryEntered.countDown();
            await(allowFactory);
            return new FakeTexture(image);
        }, manager::register, manager::release, FakeTexture::image, Runnable::run);
        FakeImage image = new FakeImage();
        entry.adopt(image);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Identifier> registration = executor.submit(() -> entry.register(FIRST_ID));
            assertTrue(factoryEntered.await(5L, TimeUnit.SECONDS));
            Future<?> close = executor.submit(entry::close);
            allowFactory.countDown();

            assertEquals(FIRST_ID, registration.get(5L, TimeUnit.SECONDS));
            close.get(5L, TimeUnit.SECONDS);
        } finally {
            allowFactory.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }

        assertTrue(manager.textures.isEmpty());
        assertEquals(1, manager.releaseCalls.get());
        assertEquals(1, image.closeCalls.get());
    }

    @Test
    void pngResourcePackIdentifierIsBorrowedAndNeverBecomesDynamicOwnership() {
        PngTexture texture = new PngTexture();
        texture.resourceLocation = FIRST_ID;

        assertEquals(FIRST_ID, texture.getResourceLocation());
        assertFalse(texture.textureEntry.canRegister());

        texture.close();

        assertEquals(PngTexture.FULLY_TRANSPARENT_TEXTURE, texture.getResourceLocation());
        assertTrue(texture.textureEntry.isClosed());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out while waiting for test coordination");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for test coordination", exception);
        }
    }

    private static final class Fixture {

        private final AtomicBoolean mainThread = new AtomicBoolean();
        private final List<Runnable> scheduledTasks = new ArrayList<>();
        private final TextureManagerReleaseDispatcher.Dispatcher dispatcher;
        private final FakeManager manager;
        private final AtomicReference<FakeTexture> createdTexture = new AtomicReference<>();
        private final TextureManagerEntry<FakeImage, FakeTexture> entry;

        private Fixture(boolean mainThread) {
            this(mainThread, new FakeManager());
        }

        private Fixture(boolean mainThread, FakeManager manager) {
            this.mainThread.set(mainThread);
            this.manager = manager;
            this.dispatcher = new TextureManagerReleaseDispatcher.Dispatcher(this.mainThread::get, this.scheduledTasks::add);
            this.entry = new TextureManagerEntry<>((identifier, image) -> {
                FakeTexture texture = new FakeTexture(image);
                this.createdTexture.set(texture);
                return texture;
            }, manager::register, manager::release, FakeTexture::image, this.dispatcher::dispatch);
        }

    }

    private static final class FakeManager {

        private final Map<Identifier, FakeTexture> textures = new ConcurrentHashMap<>();
        private final AtomicInteger releaseCalls = new AtomicInteger();
        private final AtomicBoolean failRegistration = new AtomicBoolean();

        private void register(Identifier identifier, FakeTexture texture) {
            if (this.failRegistration.get()) throw new IllegalStateException("expected test failure");
            this.textures.put(identifier, texture);
        }

        private void release(Identifier identifier) {
            this.releaseCalls.incrementAndGet();
            FakeTexture texture = this.textures.remove(identifier);
            if (texture != null) texture.close();
        }

    }

    private static final class FakeImage implements AutoCloseable {

        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

    private static final class FakeTexture implements AutoCloseable {

        private final FakeImage image;
        private final AtomicInteger closeCalls = new AtomicInteger();

        private FakeTexture(FakeImage image) {
            this.image = image;
        }

        private FakeImage image() {
            return this.image;
        }

        @Override
        public void close() {
            if (this.closeCalls.incrementAndGet() == 1) this.image.close();
        }

    }

}
