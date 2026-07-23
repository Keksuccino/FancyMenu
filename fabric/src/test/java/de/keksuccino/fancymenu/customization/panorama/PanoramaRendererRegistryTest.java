package de.keksuccino.fancymenu.customization.panorama;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PanoramaRendererRegistryTest {

    @Test
    void reloadClosesOldRendererBeforePublishingReplacement() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer oldRenderer = new RecordingRenderer("old");
        RecordingRenderer replacement = new RecordingRenderer("replacement");
        AtomicReference<RecordingRenderer> rendererVisibleDuringClose = new AtomicReference<>();
        AtomicReference<RecordingRenderer> replacementVisibleDuringClose = new AtomicReference<>();
        oldRenderer.onClose = () -> {
            rendererVisibleDuringClose.set(registry.get("panorama"));
            replacementVisibleDuringClose.set(registry.get("replacement"));
        };
        registry.replaceAll(List.of(registration("panorama", oldRenderer)));

        registry.replaceAll(List.of(registration("replacement", replacement)));

        assertEquals(1, oldRenderer.closeCount);
        assertEquals(0, replacement.closeCount);
        assertSame(oldRenderer, rendererVisibleDuringClose.get());
        assertNull(replacementVisibleDuringClose.get());
        assertNull(registry.get("panorama"));
        assertSame(replacement, registry.get("replacement"));
    }

    @Test
    void reloadClosesMultipleDetachedRenderers() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer first = new RecordingRenderer("first");
        RecordingRenderer second = new RecordingRenderer("second");
        registry.replaceAll(List.of(registration("first", first), registration("second", second)));

        registry.replaceAll(List.of(registration("third", new RecordingRenderer("third"))));

        assertEquals(1, first.closeCount);
        assertEquals(1, second.closeCount);
        assertEquals(List.of("third"), registry.names());
    }

    @Test
    void duplicateNameClosesDisplacedStagedRenderer() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer displaced = new RecordingRenderer("displaced");
        RecordingRenderer retained = new RecordingRenderer("retained");

        registry.replaceAll(List.of(registration("duplicate", displaced), registration("duplicate", retained)));

        assertEquals(1, displaced.closeCount);
        assertEquals(0, retained.closeCount);
        assertSame(retained, registry.get("duplicate"));
    }

    @Test
    void sameNameReplacementClosesPreviousRenderer() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer previous = new RecordingRenderer("previous");
        RecordingRenderer replacement = new RecordingRenderer("replacement");
        registry.replaceAll(List.of(registration("panorama", previous)));

        registry.replaceAll(List.of(registration("panorama", replacement)));

        assertEquals(1, previous.closeCount);
        assertEquals(0, replacement.closeCount);
        assertSame(replacement, registry.get("panorama"));
    }

    @Test
    void emptyReplacementClosesAndClearsRegistry() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer renderer = new RecordingRenderer("renderer");
        registry.replaceAll(List.of(registration("panorama", renderer)));

        registry.replaceAll(List.of());

        assertEquals(1, renderer.closeCount);
        assertEquals(List.of(), registry.names());
        assertEquals(List.of(), registry.values());
    }

    @Test
    void closeFailureDoesNotBlockRemainingCleanupOrPublication() {
        List<RecordingRenderer> failedRenderers = new ArrayList<>();
        PanoramaRendererRegistry<RecordingRenderer> registry = new PanoramaRendererRegistry<>((renderer, throwable) -> failedRenderers.add(renderer));
        RecordingRenderer failing = new RecordingRenderer("failing");
        failing.closeFailure = new IllegalStateException("expected failure");
        RecordingRenderer healthy = new RecordingRenderer("healthy");
        RecordingRenderer replacement = new RecordingRenderer("replacement");
        registry.replaceAll(List.of(registration("failing", failing), registration("healthy", healthy)));

        registry.replaceAll(List.of(registration("replacement", replacement)));

        assertEquals(1, failing.closeCount);
        assertEquals(1, healthy.closeCount);
        assertEquals(List.of(failing), failedRenderers);
        assertSame(replacement, registry.get("replacement"));
    }

    @Test
    void retainedIdentityStaysOpenAcrossReload() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer retained = new RecordingRenderer("retained");
        registry.replaceAll(List.of(registration("old-name", retained)));

        registry.replaceAll(List.of(registration("new-name", retained)));

        assertEquals(0, retained.closeCount);
        assertNull(registry.get("old-name"));
        assertSame(retained, registry.get("new-name"));
    }

    @Test
    void sharedAliasesAndRepeatedShutdownCloseRendererOnlyOnce() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer shared = new RecordingRenderer("shared");
        registry.replaceAll(List.of(registration("first", shared), registration("second", shared)));

        registry.close();
        registry.close();
        registry.replaceAll(List.of());

        assertEquals(1, shared.closeCount);
        assertEquals(List.of(), registry.names());
    }

    @Test
    void replacementSubmittedAfterShutdownIsClosedInsteadOfPublished() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer lateRenderer = new RecordingRenderer("late");
        registry.close();

        registry.replaceAll(List.of(registration("late", lateRenderer)));

        assertEquals(1, lateRenderer.closeCount);
        assertNull(registry.get("late"));
    }

    @Test
    void abortedReloadClosesOnlyUnpublishedRenderers() {
        PanoramaRendererRegistry<RecordingRenderer> registry = registry();
        RecordingRenderer published = new RecordingRenderer("published");
        RecordingRenderer staged = new RecordingRenderer("staged");
        registry.replaceAll(List.of(registration("published", published)));

        registry.discardUnpublished(List.of(registration("same-instance", published), registration("staged", staged), registration("staged-alias", staged)));

        assertEquals(0, published.closeCount);
        assertEquals(1, staged.closeCount);
        assertSame(published, registry.get("published"));
    }

    private static PanoramaRendererRegistry<RecordingRenderer> registry() {
        return new PanoramaRendererRegistry<>((renderer, throwable) -> {
            throw new AssertionError("Unexpected close failure for " + renderer.name, throwable);
        });
    }

    private static PanoramaRendererRegistry.Registration<RecordingRenderer> registration(String name, RecordingRenderer renderer) {
        return new PanoramaRendererRegistry.Registration<>(name, renderer);
    }

    private static final class RecordingRenderer implements AutoCloseable {

        private final String name;
        private int closeCount;
        private Runnable onClose = () -> {};
        private RuntimeException closeFailure;

        private RecordingRenderer(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            this.closeCount++;
            this.onClose.run();
            if (this.closeFailure != null) {
                throw this.closeFailure;
            }
        }

    }

}
