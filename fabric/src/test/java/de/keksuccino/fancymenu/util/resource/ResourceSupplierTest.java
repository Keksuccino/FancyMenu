package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class ResourceSupplierTest {

    private static final List<FileMediaType> RESOURCE_HANDLER_MEDIA_TYPES = List.of(FileMediaType.IMAGE, FileMediaType.AUDIO, FileMediaType.VIDEO, FileMediaType.TEXT);
    private static final List<String> EFFECTIVELY_EMPTY_SOURCES = List.of("", " ", "\t\n", "[source:location]", "[source:location]   ", "[source:location]minecraft:", "[source:location]example:", "minecraft:", "example:", "[source:local]", "[source:local]\t", "[source:web]", "[source:web]\n");
    private static final List<String> VALID_SOURCES = List.of("[source:location]example:textures/image.png", "example:textures/unprefixed.png", "[source:web]https://example.invalid/image.png");

    @Test
    void doesNotSelectOrInvokeAnyResourceHandlerForEffectivelyEmptySources() {
        withPlaceholderCachingDisabled(() -> {
            for (FileMediaType mediaType : RESOURCE_HANDLER_MEDIA_TYPES) {
                for (String source : EFFECTIVELY_EMPTY_SOURCES) {
                    RecordingResourceHandler handler = new RecordingResourceHandler();
                    RecordingResourceSupplier supplier = new RecordingResourceSupplier(mediaType, source, handler);

                    assertNull(supplier.get(), source);
                    assertEquals(0, supplier.handlerSelections, source);
                    assertEquals(0, handler.invocations, source);
                    assertFalse(ResourceSource.isDispatchable(source), source);
                }
            }
        });
    }

    @Test
    void dispatchesValidPayloadsWithoutChangingTheExistingResourceSourceContract() {
        withPlaceholderCachingDisabled(() -> {
            assertTrue(ResourceSource.isDispatchable("[source:local]config/fancymenu/image.png"));
            for (String source : VALID_SOURCES) {
                RecordingResourceHandler handler = new RecordingResourceHandler();
                RecordingResourceSupplier supplier = new RecordingResourceSupplier(FileMediaType.IMAGE, source, handler);
                ResourceSource expected = ResourceSource.of(source);

                TestResource resource = supplier.get();

                assertNotNull(resource, source);
                assertEquals(1, supplier.handlerSelections, source);
                assertEquals(1, handler.invocations, source);
                assertTrue(ResourceSource.isDispatchable(source), source);
                assertEquals(expected.getSourceType(), handler.lastSource.getSourceType(), source);
                assertEquals(expected.getSourceWithoutPrefix(), handler.lastSource.getSourceWithoutPrefix(), source);
                assertEquals(expected.getSourceWithPrefix(), handler.lastSource.getSourceWithPrefix(), source);
            }
        });
    }

    @Test
    void laterValidPlaceholderEvaluationStillDispatchesNormally() {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        String serializedSource = "[source:location]{\"placeholder\":\"randomtext\",\"values\":{\"source\":\"/config/fancymenu/assets/configs/artmenurandom.txt\",\"interval\":\"5\"}}";
        AtomicReference<String> evaluatedSource = new AtomicReference<>("[source:location]");
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, source -> serializedSource.equals(source) ? evaluatedSource.get() : source);
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            RecordingResourceHandler handler = new RecordingResourceHandler();
            RecordingResourceSupplier supplier = new RecordingResourceSupplier(FileMediaType.IMAGE, serializedSource, handler);

            assertNull(supplier.get());
            assertEquals(0, supplier.handlerSelections);
            assertEquals(0, handler.invocations);

            evaluatedSource.set("[source:location]example:textures/later.png");
            TestResource resource = supplier.get();

            assertNotNull(resource);
            assertEquals(1, supplier.handlerSelections);
            assertEquals(1, handler.invocations);
            assertEquals("[source:location]example:textures/later.png", handler.lastSource.getSourceWithPrefix());
            assertSame(resource, supplier.get());
            assertEquals(1, handler.invocations);
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    @Test
    void nonEmptyHandlerFailuresStillReachTheExistingErrorPath() {
        withPlaceholderCachingDisabled(() -> {
            RecordingResourceHandler handler = new RecordingResourceHandler();
            handler.returnLoadingFailure = true;
            RecordingResourceSupplier supplier = new RecordingResourceSupplier(FileMediaType.IMAGE, "[source:location]example:textures/malformed.png", handler);

            TestResource resource = supplier.get();

            assertNotNull(resource);
            assertTrue(resource.isLoadingFailed());
            assertEquals(1, supplier.handlerSelections);
            assertEquals(1, handler.invocations);
            assertEquals("[source:location]example:textures/malformed.png", handler.lastSource.getSourceWithPrefix());
        });
    }

    private static void withPlaceholderCachingDisabled(@NotNull Runnable test) {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            test.run();
        } finally {
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    private static final class RecordingResourceSupplier extends ResourceSupplier<TestResource> {

        private final RecordingResourceHandler handler;
        private int handlerSelections;

        private RecordingResourceSupplier(@NotNull FileMediaType mediaType, @NotNull String source, @NotNull RecordingResourceHandler handler) {
            super(TestResource.class, mediaType, source);
            this.handler = handler;
        }

        @Override
        public ResourceHandler<?, ?> getResourceHandler() {
            this.handlerSelections++;
            return this.handler;
        }
    }

    private static final class RecordingResourceHandler extends ResourceHandler<TestResource, FileType<TestResource>> {

        private int invocations;
        private ResourceSource lastSource;
        private boolean returnLoadingFailure;

        @Override
        public TestResource get(@NotNull ResourceSource source) {
            this.invocations++;
            this.lastSource = source;
            return new TestResource(this.returnLoadingFailure);
        }

        @Override
        public @NotNull List<FileType<TestResource>> getAllowedFileTypes() {
            return List.of();
        }

        @Override
        public @Nullable FileType<TestResource> getFallbackFileType() {
            return null;
        }
    }

    private static final class TestResource implements Resource {

        private final boolean loadingFailed;
        private boolean closed;

        private TestResource(boolean loadingFailed) {
            this.loadingFailed = loadingFailed;
        }

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isLoadingCompleted() {
            return !this.loadingFailed;
        }

        @Override
        public boolean isLoadingFailed() {
            return this.loadingFailed;
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }
}
