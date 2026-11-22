package de.keksuccino.fancymenu.util.resource.resources.texture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class TextureManagerReleaserTest {

    private static final ResourceLocation FIRST_ID = new ResourceLocation("fancymenu", "dynamic/test_first");
    private static final ResourceLocation SECOND_ID = new ResourceLocation("fancymenu", "dynamic/test_second");

    private final Map<ResourceLocation, AbstractTexture> byPath = new HashMap<>();
    private final Set<Tickable> tickableTextures = new HashSet<>();
    private final FakeTexture missingTexture = new FakeTexture();

    @Test
    void releaseRemovesTheEntryAndClosesItsTextureOnce() {
        FakeTexture texture = new FakeTexture();
        this.byPath.put(FIRST_ID, texture);

        this.release(FIRST_ID);
        this.release(FIRST_ID);

        assertFalse(this.byPath.containsKey(FIRST_ID));
        assertEquals(1, texture.closeCalls);
        assertEquals(1, texture.releaseIdCalls);
    }

    @Test
    void releaseLeavesOtherEntriesAlone() {
        FakeTexture released = new FakeTexture();
        FakeTexture kept = new FakeTexture();
        this.byPath.put(FIRST_ID, released);
        this.byPath.put(SECOND_ID, kept);

        this.release(FIRST_ID);

        assertSame(kept, this.byPath.get(SECOND_ID));
        assertEquals(1, this.byPath.size());
        assertEquals(0, kept.closeCalls);
        assertEquals(0, kept.releaseIdCalls);
    }

    @Test
    void releaseStopsTickingTheTexture() {
        FakeTickableTexture released = new FakeTickableTexture();
        FakeTickableTexture kept = new FakeTickableTexture();
        this.byPath.put(FIRST_ID, released);
        this.byPath.put(SECOND_ID, kept);
        this.tickableTextures.add(released);
        this.tickableTextures.add(kept);

        this.release(FIRST_ID);

        assertEquals(Set.of(kept), this.tickableTextures);
        assertEquals(1, released.closeCalls);
    }

    @Test
    void releaseOfTheSharedMissingTextureOnlyRemovesItsEntry() {
        this.byPath.put(FIRST_ID, this.missingTexture);
        this.byPath.put(SECOND_ID, this.missingTexture);

        this.release(FIRST_ID);

        assertFalse(this.byPath.containsKey(FIRST_ID));
        assertSame(this.missingTexture, this.byPath.get(SECOND_ID));
        assertEquals(0, this.missingTexture.closeCalls);
        assertEquals(0, this.missingTexture.releaseIdCalls);
    }

    @Test
    void releaseOfAnUnknownLocationChangesNothing() {
        FakeTexture kept = new FakeTexture();
        this.byPath.put(SECOND_ID, kept);

        this.release(FIRST_ID);

        assertSame(kept, this.byPath.get(SECOND_ID));
        assertEquals(0, kept.closeCalls);
        assertEquals(0, kept.releaseIdCalls);
    }

    @Test
    void failingCloseStillRemovesTheEntryAndFreesTheGlTexture() {
        FakeTexture texture = new FakeTexture();
        texture.failClose = true;
        this.byPath.put(FIRST_ID, texture);

        try (LogCapture log = LogCapture.problemsOf(TextureManagerReleaser.class)) {
            this.release(FIRST_ID);
            assertEquals(1, log.messages().size());
        }

        assertFalse(this.byPath.containsKey(FIRST_ID));
        assertEquals(1, texture.closeCalls);
        assertEquals(1, texture.releaseIdCalls);
    }

    private void release(@NotNull ResourceLocation location) {
        TextureManagerReleaser.release(this.byPath, this.tickableTextures, location, this.missingTexture);
    }

    /**
     * Counts what the release does with it. Overrides releaseId(), because the real one needs the render system.
     */
    private static class FakeTexture extends AbstractTexture {

        int closeCalls;
        int releaseIdCalls;
        boolean failClose;

        @Override
        public void load(@NotNull ResourceManager resourceManager) {
        }

        @Override
        public void close() {
            this.closeCalls++;
            if (this.failClose) throw new IllegalStateException("expected test failure");
        }

        @Override
        public void releaseId() {
            this.releaseIdCalls++;
        }

    }

    private static class FakeTickableTexture extends FakeTexture implements Tickable {

        @Override
        public void tick() {
        }

    }

}
