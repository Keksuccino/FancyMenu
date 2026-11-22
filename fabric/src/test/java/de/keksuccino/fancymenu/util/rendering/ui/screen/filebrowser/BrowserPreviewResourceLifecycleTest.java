package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.testing.FakeResourceHandler;
import de.keksuccino.fancymenu.util.file.type.FileTypeRegistry;
import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourcePickerWindowBody;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Previews of a real resource picker, loaded by its own loader through fake resource handlers that replace the global ones.<br>
 * The picker skips its constructor, because the constructor of Minecraft's Screen reads the font of the game, which doesn't exist in unit tests.
 */
@ResourceLock("ResourceHandlers global state")
class BrowserPreviewResourceLifecycleTest {

    private static final Unsafe UNSAFE = loadUnsafe();

    private final FakeResourceHandler<IAudio, AudioFileType> audioHandler = FakeResourceHandler.audio("ogg");
    private final FakeResourceHandler<ITexture, ImageFileType> imageHandler = FakeResourceHandler.image("png");
    private final FakeResourceHandler<IVideo, VideoFileType> videoHandler = FakeResourceHandler.video("mp4");
    private final FakeResourceHandler<IText, TextFileType> textHandler = FakeResourceHandler.text("txt");
    private ResourceHandler<IAudio, AudioFileType> originalAudioHandler;
    private ResourceHandler<ITexture, ImageFileType> originalImageHandler;
    private ResourceHandler<IVideo, VideoFileType> originalVideoHandler;
    private ResourceHandler<IText, TextFileType> originalTextHandler;

    /**
     * The picker tells audio, image, video and text locations apart by the registered file types, which the game registers at launch.
     * The registry can't remove file types, so they stay registered for the rest of the test JVM, like they do in the game.
     */
    @BeforeAll
    static void registerFileTypesLikeTheGame() {
        if (FileTypeRegistry.getFileType("ogg") == null) FileTypes.registerAll();
    }

    @BeforeEach
    void useFakeResourceHandlers() {
        this.originalAudioHandler = ResourceHandlers.getAudioHandler();
        this.originalImageHandler = ResourceHandlers.getImageHandler();
        this.originalVideoHandler = ResourceHandlers.getVideoHandler();
        this.originalTextHandler = ResourceHandlers.getTextHandler();
        ResourceHandlers.setAudioHandler(this.audioHandler);
        ResourceHandlers.setImageHandler(this.imageHandler);
        ResourceHandlers.setVideoHandler(this.videoHandler);
        ResourceHandlers.setTextHandler(this.textHandler);
    }

    @AfterEach
    void restoreResourceHandlers() {
        ResourceHandlers.setAudioHandler(this.originalAudioHandler);
        ResourceHandlers.setImageHandler(this.originalImageHandler);
        ResourceHandlers.setVideoHandler(this.originalVideoHandler);
        ResourceHandlers.setTextHandler(this.originalTextHandler);
    }

    @Test
    void browsingThroughAudiosKeepsOnlyTheCurrentPreviewLoaded() {
        TestResourcePicker picker = allocatePicker();
        List<IAudio> previews = new ArrayList<>();

        for (String location : List.of("test:sounds/first.ogg", "test:sounds/second.ogg", "test:sounds/third.ogg", "test:sounds/fourth.ogg")) {
            select(picker, location);
            IAudio preview = previewAudio(picker);
            assertNotNull(preview, location);
            preview.play();
            previews.add(preview);
            assertEquals(1, this.audioHandler.countOpenResources(), location);
        }

        for (IAudio preview : previews.subList(0, previews.size() - 1)) {
            assertTrue(preview.isClosed());
            assertFalse(preview.isPlaying());
        }
        assertFalse(previews.get(previews.size() - 1).isClosed());
        assertEquals(previews.size(), this.audioHandler.getCreatedResources().size());
    }

    @Test
    void deselectingClosesThePreview() {
        TestResourcePicker picker = allocatePicker();
        select(picker, "test:sounds/first.ogg");
        IAudio preview = previewAudio(picker);
        assertNotNull(preview);

        // Clicking empty space, a folder or the go-up entry, and changing the directory all deselect the preview
        select(picker, null);

        assertTrue(preview.isClosed());
        assertNull(picker.previewAudioSupplier);
        assertEquals(0, this.audioHandler.countOpenResources());
    }

    @Test
    void switchingToAnotherKindOfPreviewClosesThePreviewBefore() {
        TestResourcePicker picker = allocatePicker();

        select(picker, "test:textures/image.png");
        ITexture texture = Objects.requireNonNull(picker.previewTextureSupplier).get();
        assertNotNull(texture);
        select(picker, "test:sounds/first.ogg");
        IAudio audio = previewAudio(picker);
        assertNotNull(audio);
        assertTrue(texture.isClosed());
        select(picker, "test:videos/video.mp4");
        IVideo video = Objects.requireNonNull(picker.previewVideoSupplier).get();
        assertNotNull(video);
        assertTrue(audio.isClosed());
        select(picker, "test:texts/text.txt");
        IText text = Objects.requireNonNull(picker.previewTextSupplier).get();
        assertNotNull(text);
        assertTrue(video.isClosed());
        select(picker, null);

        assertTrue(text.isClosed());
        assertEquals(0, this.imageHandler.countOpenResources() + this.audioHandler.countOpenResources() + this.videoHandler.countOpenResources() + this.textHandler.countOpenResources());
    }

    @Test
    void closingTheBrowserClosesThePreview() {
        TestResourcePicker picker = allocatePicker();
        select(picker, "test:sounds/first.ogg");
        IAudio preview = previewAudio(picker);
        assertNotNull(preview);
        preview.play();

        picker.onScreenClosed();

        assertTrue(preview.isClosed());
        assertFalse(preview.isPlaying());
        assertNull(picker.previewAudioSupplier);
    }

    @Test
    void closingTheBrowserDropsAPreviewThatStillWaitsForItsDelay() {
        TestResourcePicker picker = allocatePicker();
        ResourceLocation location = ResourceLocation.tryParse("test:sounds/first.ogg");
        picker.selected = location;
        picker.updatePreview(location);

        picker.onScreenClosed();
        loadPendingPreviewNow(picker);

        assertNull(picker.previewAudioSupplier);
        assertEquals(0, this.audioHandler.getCodecReads());
    }

    @Test
    void releasingThePreviewNeverClosesTheSharedAudioOfTheSameFile() {
        // Like an audio element on the current screen that plays the same file
        IAudio shared = this.audioHandler.get("test:sounds/first.ogg");
        assertNotNull(shared);
        shared.play();
        shared.setVolume(0.25F);
        TestResourcePicker picker = allocatePicker();

        select(picker, "test:sounds/first.ogg");
        IAudio preview = previewAudio(picker);
        assertNotNull(preview);
        assertNotSame(shared, preview);
        preview.play();
        preview.setVolume(1.0F);
        select(picker, "test:sounds/second.ogg");
        assertTrue(preview.isClosed());
        picker.onScreenClosed();

        assertFalse(shared.isClosed());
        assertTrue(shared.isPlaying());
        assertEquals(0.25F, shared.getVolume());
        assertSame(shared, this.audioHandler.get("test:sounds/first.ogg"));
        assertEquals(1, this.audioHandler.countOpenResources());
    }

    @Nullable
    private static IAudio previewAudio(@NotNull TestResourcePicker picker) {
        return Objects.requireNonNull(picker.previewAudioSupplier).get();
    }

    /**
     * Selects the location like a click on its entry does, and loads its preview right away instead of after the preview delay.
     */
    private static void select(@NotNull TestResourcePicker picker, @Nullable String location) {
        ResourceLocation identifier = (location != null) ? ResourceLocation.tryParse(location) : null;
        picker.selected = identifier;
        picker.updatePreview(identifier);
        loadPendingPreviewNow(picker);
    }

    private static void loadPendingPreviewNow(@NotNull TestResourcePicker picker) {
        picker.pendingPreviewLoadAtMs = 0L;
        picker.tickPreviewDelay();
    }

    @NotNull
    private static TestResourcePicker allocatePicker() {
        try {
            return (TestResourcePicker) UNSAFE.allocateInstance(TestResourcePicker.class);
        } catch (InstantiationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NotNull
    private static Unsafe loadUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets its selected entry from the test, because its entry list needs the UI of the game.
     */
    private static final class TestResourcePicker extends ResourcePickerWindowBody {

        @Nullable
        private ResourceLocation selected;

        private TestResourcePicker() {
            super(null, null, location -> {});
        }

        @Override
        protected @Nullable Object getSelectedPreviewKey() {
            return this.selected;
        }

    }

}
