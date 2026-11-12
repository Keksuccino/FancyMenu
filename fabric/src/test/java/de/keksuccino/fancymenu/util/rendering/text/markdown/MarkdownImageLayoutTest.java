package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownImageLayoutTest {

    private static final int CHAR_WIDTH = 6;
    private static final String WIDE_WORD = "a".repeat(60);

    @Test
    void imageSmallerThanTheTextKeepsItsSize() {
        TestImage image = new TestImage(100, 50);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        assertEquals(100, fragment.getRenderWidth());
        assertEquals(50, fragment.getRenderHeight());
        assertEquals(50, fragment.parentLine.getLineHeight());
        assertRealHeightMatchesLines(renderer);
    }

    @Test
    void imageWiderThanTheTextShrinksToTheWidthOfTheText() {
        TestImage image = new TestImage(600, 300);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        assertEquals(196, fragment.getRenderWidth());
        assertEquals(98, fragment.getRenderHeight());
        assertEquals(200, renderer.getRealWidth());
        assertRealHeightMatchesLines(renderer);
    }

    @Test
    void realHeightCoversAnImageBeforeALineThatIsWiderThanTheOptimalWidth() {
        TestImage image = new TestImage(600, 300);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\n" + WIDE_WORD, 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        float textWidth = WIDE_WORD.length() * CHAR_WIDTH;

        assertEquals(textWidth + 4, renderer.getRealWidth());
        assertEquals(textWidth, fragment.getRenderWidth());
        assertEquals(textWidth / 2, fragment.getRenderHeight());
        assertEquals(textWidth / 2, fragment.parentLine.getLineHeight());
        assertRealHeightMatchesLines(renderer);
    }

    @Test
    void realHeightCoversAnImageAfterALineThatIsWiderThanTheOptimalWidth() {
        TestImage image = new TestImage(600, 300);
        MarkdownTestRenderer renderer = rendererWithImage(WIDE_WORD + "\nIMAGE", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        float textWidth = WIDE_WORD.length() * CHAR_WIDTH;

        assertEquals(textWidth, fragment.getRenderWidth());
        assertEquals(textWidth / 2, fragment.parentLine.getLineHeight());
        assertRealHeightMatchesLines(renderer);
    }

    @Test
    void imageGetsItsSizeWhenItsTextureFinishesLoading() {
        TestImage image = new TestImage(0, 0);
        image.texture = null;
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        float textLineOffset = renderer.fragmentContaining("text").parentLine.offsetY;
        assertEquals(10, fragment.parentLine.getLineHeight());
        int layouts = renderer.layouts;

        image.texture = new TestTexture(100, 50);
        renderer.tick();

        assertEquals(50, fragment.parentLine.getLineHeight());
        assertEquals(100, fragment.parentLine.getLineWidth());
        assertEquals(textLineOffset + 40, renderer.fragmentContaining("text").parentLine.offsetY);
        assertEquals(renderer.getY() + textLineOffset + 40, renderer.fragmentContaining("text").y);
        assertRealHeightMatchesLines(renderer);
        // Lines wrap the same with every image size, so the lines only get measured and positioned again
        assertEquals(layouts, renderer.layouts);
    }

    @Test
    void imageGetsMeasuredAgainWhenTheSizeOfItsTextureChanges() {
        TestImage image = new TestImage(100, 50);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        float realHeight = renderer.getRealHeight();

        ((TestTexture) image.texture).height = 80;
        renderer.tick();

        assertEquals(80, fragment.parentLine.getLineHeight());
        assertEquals(realHeight + 30, renderer.getRealHeight());
    }

    @Test
    void unchangedImagesDoNotGetMeasuredAgain() {
        TestImage image = new TestImage(100, 50);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        // The next measurement would show this, so the size staying the same proves that nothing measured the line again
        fragment.parentLine.lineHeight = 1234;
        renderer.tick();
        renderer.getRealHeight();

        assertEquals(1234, fragment.parentLine.getLineHeight());
    }

    private static MarkdownTestRenderer rendererWithImage(String text, float optimalWidth, TestImage image) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, optimalWidth);
        renderer.tick();
        // Images load through the game's resource system, so the test turns a text fragment into an image fragment
        renderer.fragmentContaining("IMAGE").imageSupplier = image;
        renderer.setOptimalWidth(optimalWidth + 1);
        renderer.tick();
        renderer.setOptimalWidth(optimalWidth);
        renderer.tick();
        return renderer;
    }

    private static void assertRealHeightMatchesLines(MarkdownTestRenderer renderer) {
        float height = 0;
        for (MarkdownTextLine line : renderer.lines) {
            float lineHeight = 0;
            for (MarkdownTextFragment fragment : line.fragments) lineHeight = Math.max(lineHeight, fragment.getRenderHeight());
            assertEquals(lineHeight, line.getLineHeight());
            height += lineHeight + renderer.getLineSpacing();
        }
        assertEquals(height + (renderer.getBorder() * 2), renderer.getRealHeight());
        MarkdownTextLine lastLine = renderer.lines.get(renderer.lines.size() - 1);
        assertTrue((lastLine.offsetY + lastLine.getLineHeight()) <= renderer.getRealHeight());
    }

    private static final class TestImage extends ResourceSupplier<ITexture> {

        @Nullable
        private ITexture texture;

        private TestImage(int width, int height) {
            super(ITexture.class, FileMediaType.IMAGE, "test");
            this.texture = new TestTexture(width, height);
        }

        @Override
        public @Nullable ITexture get() {
            return this.texture;
        }

    }

    private static final class TestTexture implements ITexture {

        private int width;
        private int height;

        private TestTexture(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public @Nullable Identifier getResourceLocation() {
            return null;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public @NotNull AspectRatio getAspectRatio() {
            return new AspectRatio(this.width, this.height);
        }

        @Override
        public void reset() {
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
            return true;
        }

        @Override
        public boolean isLoadingFailed() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
        }

    }

}
