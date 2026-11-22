package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.testing.FakeResourceHandler;
import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnedResourceSupplierTest {

    private static final String SOURCE = "[source:location]test:sounds/preview.ogg";
    private static final String OTHER_SOURCE = "[source:location]test:sounds/other.ogg";

    @Test
    void createsAnOwnInstanceThatTheSharedCacheNeverHandsOut() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        IAudio shared = handler.get(SOURCE);
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);
        OwnedResourceSupplier<IAudio> otherSupplier = owned(handler, OTHER_SOURCE);

        IAudio owned = supplier.get();
        IAudio otherOwned = otherSupplier.get();

        assertNotNull(shared);
        assertNotNull(owned);
        assertNotNull(otherOwned);
        assertNotSame(shared, owned);
        assertSame(shared, handler.get(SOURCE));
        assertFalse(handler.hasResource(ResourceSource.of(OTHER_SOURCE).getSourceWithPrefix()));
        assertNotSame(otherOwned, handler.get(OTHER_SOURCE));
    }

    @Test
    void closingReleasesTheOwnedResourceAndNeverTouchesTheSharedOne() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        IAudio shared = handler.get(SOURCE);
        assertNotNull(shared);
        shared.play();
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);
        IAudio owned = supplier.get();
        assertNotNull(owned);
        owned.play();

        supplier.close();

        assertTrue(owned.isClosed());
        assertFalse(owned.isPlaying());
        assertFalse(shared.isClosed());
        assertTrue(shared.isPlaying());
        assertSame(shared, handler.get(SOURCE));
    }

    @Test
    void returnsAndCreatesNothingOnceItGotClosed() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        OwnedResourceSupplier<IAudio> unusedSupplier = owned(handler, SOURCE);
        unusedSupplier.close();
        assertNull(unusedSupplier.get());
        assertEquals(0, handler.getCodecReads());

        OwnedResourceSupplier<IAudio> usedSupplier = owned(handler, SOURCE);
        assertNotNull(usedSupplier.get());
        usedSupplier.close();
        usedSupplier.close();

        assertNull(usedSupplier.get());
        assertEquals(1, handler.getCreatedResources().size());
        assertEquals(1, handler.getCreatedResources().get(0).getCloseCalls());
    }

    @Test
    void createsTheResourceOnlyOnceWhileItStaysOpen() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);

        IAudio first = supplier.get();
        IAudio second = supplier.get();

        assertNotNull(first);
        assertSame(first, second);
        assertEquals(1, handler.getCodecReads());
    }

    @Test
    void createsANewResourceAfterSomethingElseClosedIt() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);
        IAudio first = supplier.get();
        assertNotNull(first);

        // Like the sound engine reload, which closes every audio
        assertSame(first, handler.getCreatedResources().get(0));
        handler.getCreatedResources().get(0).close();
        IAudio second = supplier.get();

        assertNotNull(second);
        assertNotSame(first, second);
        assertFalse(second.isClosed());
        assertSame(second, supplier.get());
        assertEquals(2, handler.getCodecReads());
    }

    @Test
    void triesSourcesThatCantBeCreatedOnlyOnceWithoutMarkingThemAsFailedForTheSharedCache() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        handler.setCodecReturnsNull(true);
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);

        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(1, handler.getCodecReads());

        handler.setCodecReturnsNull(false);
        assertNotNull(handler.get(SOURCE));
        assertEquals(2, handler.getCodecReads());
    }

    @Test
    void neverReadsUnsupportedOrEmptySources() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");

        assertNull(owned(handler, "[source:location]test:textures/image.png").get());
        assertNull(owned(handler, "[source:location]").get());
        assertNull(owned(handler, "").get());
        assertEquals(0, handler.getCodecReads());
    }

    @Test
    void reloadingAllResourcesKeepsOwnedResourcesOpen() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        IAudio shared = handler.get(SOURCE);
        OwnedResourceSupplier<IAudio> supplier = owned(handler, SOURCE);
        IAudio owned = supplier.get();
        assertNotNull(shared);
        assertNotNull(owned);

        handler.releaseAll();

        assertTrue(shared.isClosed());
        assertFalse(owned.isClosed());
        assertSame(owned, supplier.get());
    }

    @Test
    void shutdownClosesOwnedResourcesThatAreStillOpenAndRejectsNewOnes() {
        FakeResourceHandler<IAudio, AudioFileType> handler = FakeResourceHandler.audio("ogg");
        OwnedResourceSupplier<IAudio> openSupplier = owned(handler, SOURCE);
        OwnedResourceSupplier<IAudio> closedSupplier = owned(handler, OTHER_SOURCE);
        assertNotNull(openSupplier.get());
        assertNotNull(closedSupplier.get());
        closedSupplier.close();

        handler.shutdown();

        FakeResourceHandler.FakeResource open = handler.getCreatedResources().get(0);
        FakeResourceHandler.FakeResource closed = handler.getCreatedResources().get(1);
        assertTrue(open.isClosed());
        assertEquals(1, open.getCloseCalls());
        assertEquals(1, closed.getCloseCalls());
        assertNull(handler.createOwned(ResourceSource.of(SOURCE)));
        assertNull(owned(handler, SOURCE).get());
        assertEquals(2, handler.getCodecReads());
    }

    @NotNull
    private static OwnedResourceSupplier<IAudio> owned(@NotNull FakeResourceHandler<IAudio, AudioFileType> handler, @NotNull String source) {
        return new OwnedResourceSupplier<>(() -> handler, source);
    }

}
