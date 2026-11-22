package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.testing.FakeResourceHandler;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resource.OwnedResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Previews of the screen-based file browser, which owns its image and text previews like the browser windows do.<br>
 * The screen skips its constructor, because the constructor of Minecraft's Screen reads the font of the game, which doesn't exist in unit tests.
 */
class FileBrowserScreenPreviewResourceLifecycleTest {

    private static final Unsafe UNSAFE = loadUnsafe();
    private static final String IMAGE_SOURCE = "[source:location]test:textures/first.png";
    private static final String OTHER_IMAGE_SOURCE = "[source:location]test:textures/second.png";
    private static final String TEXT_SOURCE = "[source:location]test:texts/text.txt";

    private final FakeResourceHandler<ITexture, ImageFileType> imageHandler = FakeResourceHandler.image("png");
    private final FakeResourceHandler<IText, TextFileType> textHandler = FakeResourceHandler.text("txt");

    @Test
    void replacingAPreviewClosesThePreviewBefore() {
        TestFileBrowserScreen screen = allocateScreen();
        OwnedResourceSupplier<ITexture> firstSupplier = new OwnedResourceSupplier<>(() -> this.imageHandler, IMAGE_SOURCE);
        screen.setPreviewTexture(firstSupplier);
        ITexture first = firstSupplier.get();
        assertNotNull(first);

        OwnedResourceSupplier<ITexture> secondSupplier = new OwnedResourceSupplier<>(() -> this.imageHandler, OTHER_IMAGE_SOURCE);
        screen.setPreviewTexture(secondSupplier);
        ITexture second = secondSupplier.get();
        // Setting the same preview again must not close it
        screen.setPreviewTexture(secondSupplier);

        assertTrue(first.isClosed());
        assertNotNull(second);
        assertFalse(second.isClosed());
        assertEquals(1, this.imageHandler.countOpenResources());
    }

    @Test
    void deselectingTheFileClosesItsPreviews() {
        TestFileBrowserScreen screen = allocateScreen();
        OwnedResourceSupplier<ITexture> textureSupplier = new OwnedResourceSupplier<>(() -> this.imageHandler, IMAGE_SOURCE);
        OwnedResourceSupplier<IText> textSupplier = new OwnedResourceSupplier<>(() -> this.textHandler, TEXT_SOURCE);
        screen.setPreviewTexture(textureSupplier);
        screen.setPreviewText(textSupplier);
        ITexture texture = textureSupplier.get();
        IText text = textSupplier.get();
        assertNotNull(texture);
        assertNotNull(text);

        // Clicking empty space or a folder deselects the file
        screen.updatePreview(null);

        assertTrue(texture.isClosed());
        assertTrue(text.isClosed());
        assertNull(screen.previewTextureSupplier);
        assertNull(screen.previewTextSupplier);
        assertEquals(0, this.imageHandler.countOpenResources() + this.textHandler.countOpenResources());
    }

    @Test
    void closingTheScreenClosesThePreviewButNeverTheSharedResourceOfTheSameFile() {
        // Like an image element on a screen that shows the same file
        ITexture shared = this.imageHandler.get(IMAGE_SOURCE);
        assertNotNull(shared);
        TestFileBrowserScreen screen = allocateScreen();
        OwnedResourceSupplier<ITexture> previewSupplier = new OwnedResourceSupplier<>(() -> this.imageHandler, IMAGE_SOURCE);
        screen.setPreviewTexture(previewSupplier);
        ITexture preview = previewSupplier.get();
        assertNotNull(preview);
        assertNotSame(shared, preview);

        screen.removed();

        assertTrue(preview.isClosed());
        assertNull(screen.previewTextureSupplier);
        assertFalse(shared.isClosed());
        assertSame(shared, this.imageHandler.get(IMAGE_SOURCE));
        assertEquals(1, this.imageHandler.countOpenResources());
    }

    @NotNull
    private static TestFileBrowserScreen allocateScreen() {
        try {
            return (TestFileBrowserScreen) UNSAFE.allocateInstance(TestFileBrowserScreen.class);
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

    private static final class TestFileBrowserScreen extends AbstractFileBrowserScreen {

        private TestFileBrowserScreen() {
            super(Component.empty(), null, new File("."), file -> {});
        }

        @Override
        protected @NotNull ExtendedButton buildConfirmButton() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @NotNull AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f) {
            throw new UnsupportedOperationException();
        }

    }

}
