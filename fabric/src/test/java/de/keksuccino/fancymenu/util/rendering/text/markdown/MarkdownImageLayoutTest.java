package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownImageAlignment;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    /**
     * Textures load on another thread, so the size of an image can arrive at any moment, also while the layout measures the line of the image.
     * The texture of this test gets its size at a different one of the looks the layout takes at it in every round.
     *
     * @param readsBetweenValues How many looks at the texture lie between the arrival of its width, its height and its aspect ratio. They arrive together with 0.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void imageThatFinishesLoadingWhileItsLineGetsMeasuredStillGetsItsSize(int readsBetweenValues) {
        for (int loadsAtRead = 1; loadsAtRead <= 60; loadsAtRead++) {
            String round = "loads at read " + loadsAtRead;
            TestImage image = new TestImage(0, 0);
            LoadingTexture texture = new LoadingTexture(100, 50, readsBetweenValues);
            image.texture = texture;
            MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
            MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
            float textLineOffset = renderer.fragmentContaining("text").parentLine.offsetY;
            assertEquals(10, fragment.parentLine.getLineHeight(), round);

            texture.readsUntilLoading = loadsAtRead;
            // The layout measures all lines again, and looks at the image once per frame after that
            renderer.setOptimalWidth(201);
            for (int frame = 0; !texture.isLoaded(); frame++) {
                assertTrue(frame < 100, round);
                renderer.tick();
            }
            renderer.tick();

            assertEquals(50, fragment.parentLine.getLineHeight(), round);
            assertEquals(100, fragment.parentLine.getLineWidth(), round);
            assertEquals(textLineOffset + 40, renderer.fragmentContaining("text").parentLine.offsetY, round);
        }
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

    @Test
    void pixelLookEnlargesImagesByWholeFactorsAndFramesAndCentersThem() {
        TestImage image = new TestImage(96, 44);
        MarkdownTestRenderer renderer = pixelRendererWithImage("IMAGE\ntext", 238, image, "The Glowmoss Caves");
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        // The text area is 230 pixels wide, and the frame takes 2 pixels on every side, so the image fits twice but not three times
        assertEquals(192, MarkdownImagePainter.getImageWidth(fragment));
        assertEquals(88, MarkdownImagePainter.getImageHeight(fragment));
        assertEquals(192 + 4, fragment.getRenderWidth());
        // The caption stands 3 pixels below the frame
        assertEquals(88 + 4 + 3 + 9, fragment.getRenderHeight());
        assertEquals(MarkdownRenderer.MarkdownLineAlignment.CENTERED, fragment.parentLine.alignment);
        assertEquals(4 + ((230 - 196) / 2), fragment.parentLine.offsetX);
        assertEquals(1, fragment.imageCaption.lines.length);
        assertEquals("The Glowmoss Caves", fragment.imageCaption.lines[0].getString());
        assertRealHeightCoversLastLine(renderer);
    }

    @Test
    void wholeMultipleNeverGoesAboveTheMaximumFactorOrBelowOne() {
        assertEquals(2, MarkdownImagePainter.getWholeMultiple(96, 226, 3));
        assertEquals(3, MarkdownImagePainter.getWholeMultiple(96, 288, 3));
        assertEquals(3, MarkdownImagePainter.getWholeMultiple(96, 5000, 3));
        assertEquals(2, MarkdownImagePainter.getWholeMultiple(96, 287.9F, 3));
        assertEquals(1, MarkdownImagePainter.getWholeMultiple(96, 50, 3));
        assertEquals(1, MarkdownImagePainter.getWholeMultiple(96, 5000, 1));
        assertEquals(2, MarkdownImagePainter.getWholeMultiple(96, 5000, 2.9F));
        assertEquals(1, MarkdownImagePainter.getWholeMultiple(96, 5000, 0));
        assertEquals(1, MarkdownImagePainter.getWholeMultiple(0, 5000, 3));
    }

    @Test
    void imageThatIsTooWideForAWholeFactorShrinksToTheTextArea() {
        TestImage image = new TestImage(600, 300);
        MarkdownTestRenderer renderer = pixelRendererWithImage("IMAGE\ntext", 238, image, null);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        assertEquals(226, MarkdownImagePainter.getImageWidth(fragment));
        assertEquals(113, MarkdownImagePainter.getImageHeight(fragment));
        assertEquals(230, fragment.getRenderWidth());
        assertEquals(113 + 4, fragment.getRenderHeight());
        assertEquals(238, renderer.getRealWidth());
    }

    @Test
    void captionWrapsInsideTheTextArea() {
        TestImage image = new TestImage(96, 44);
        MarkdownTestRenderer renderer = pixelRendererWithImage("IMAGE\ntext", 238, image, "one two three four five six seven eight nine ten eleven twelve thirteen fourteen");
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        MarkdownImagePainter.Caption caption = fragment.imageCaption;

        assertTrue(caption.lines.length > 1);
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < caption.lines.length; i++) {
            assertTrue(caption.widths[i] <= 230, "caption line is wider than the text area");
            assertEquals(caption.lines[i].getString().length() * CHAR_WIDTH, caption.widths[i]);
            words.append((i > 0) ? " " : "").append(caption.lines[i].getString());
        }
        assertEquals("one two three four five six seven eight nine ten eleven twelve thirteen fourteen", words.toString());
        // Lines of the caption are a line spacing apart
        assertEquals(88 + 4 + 3 + (caption.lines.length * 9) + ((caption.lines.length - 1) * 2), fragment.getRenderHeight());
        assertRealHeightCoversLastLine(renderer);
    }

    @Test
    void captionOnlyShowsIfTheStyleSaysSoAndTheImageHasAnAlternativeText() {
        TestImage image = new TestImage(96, 44);
        MarkdownTestRenderer renderer = pixelRendererWithImage("IMAGE\ntext", 238, image, "   ");
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        assertEquals(null, fragment.imageCaption);

        fragment.imageAltText = "caption";
        renderer.getStyle().imageCaption.set(false);
        renderer.tick();
        assertEquals(null, fragment.imageCaption);
        assertEquals(88 + 4, fragment.getRenderHeight());

        renderer.getStyle().imageCaption.set(true);
        renderer.tick();
        assertEquals("caption", fragment.imageCaption.lines[0].getString());
    }

    @Test
    void imageAlignmentOfTheStyleOverridesTheAlignmentOfTheText() {
        TestImage image = new TestImage(100, 50);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");
        assertEquals(2, fragment.parentLine.offsetX);

        renderer.getStyle().imageAlignment.set(MarkdownImageAlignment.RIGHT);
        renderer.tick();
        assertEquals(2 + 196 - 100, renderer.fragmentContaining("IMAGE").parentLine.offsetX);

        renderer.getStyle().imageAlignment.set(MarkdownImageAlignment.CENTER);
        renderer.tick();
        assertEquals(2 + 48, renderer.fragmentContaining("IMAGE").parentLine.offsetX);
    }

    @Test
    void frameAddsToTheSizeOfAnImageAtItsOwnSize() {
        TestImage image = new TestImage(100, 50);
        MarkdownTestRenderer renderer = rendererWithImage("IMAGE\ntext", 200, image);
        renderer.getStyle().imageFrameWidth.set(1.5F);
        renderer.tick();
        MarkdownTextFragment fragment = renderer.fragmentContaining("IMAGE");

        assertEquals(100, MarkdownImagePainter.getImageWidth(fragment));
        assertEquals(100 + 6, fragment.getRenderWidth());
        assertEquals(50 + 6, fragment.getRenderHeight());
    }

    private static MarkdownTestRenderer pixelRendererWithImage(String text, float optimalWidth, TestImage image, @Nullable String altText) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, optimalWidth);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();
        renderer.fragmentContaining("IMAGE").imageSupplier = image;
        renderer.fragmentContaining("IMAGE").imageAltText = altText;
        renderer.setOptimalWidth(optimalWidth + 1);
        renderer.tick();
        renderer.setOptimalWidth(optimalWidth);
        renderer.tick();
        return renderer;
    }

    private static void assertRealHeightCoversLastLine(MarkdownTestRenderer renderer) {
        MarkdownTextLine lastLine = renderer.lines.get(renderer.lines.size() - 1);
        assertEquals(lastLine.offsetY + lastLine.getLineHeight() + 4, renderer.getRealHeight());
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
            assertEquals(line.paddingTop + lineHeight + line.paddingBottom, line.getLineHeight());
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

    /**
     * A texture that loads the way the textures of the game do: it has the size of a placeholder until its width, its height and
     * its aspect ratio arrive one after the other. Another thread sets them there, which can happen between any two looks at the texture.
     */
    private static final class LoadingTexture extends TestTexture {

        private final int loadedWidth;
        private final int loadedHeight;
        private final int readsBetweenValues;
        private AspectRatio aspectRatio = new AspectRatio(10, 10);
        /** How many looks at the texture are left until its width arrives. It does not load while this is 0. */
        private int readsUntilLoading = 0;
        private int valuesToArrive = 3;
        private int readsUntilNextValue = 0;

        private LoadingTexture(int loadedWidth, int loadedHeight, int readsBetweenValues) {
            super(10, 10);
            this.loadedWidth = loadedWidth;
            this.loadedHeight = loadedHeight;
            this.readsBetweenValues = readsBetweenValues;
        }

        private boolean isLoaded() {
            return this.valuesToArrive == 0;
        }

        private void onRead() {
            if ((this.readsUntilLoading > 0) && (--this.readsUntilLoading == 0)) this.readsUntilNextValue = 1;
            if ((this.readsUntilNextValue == 0) || (--this.readsUntilNextValue > 0)) return;
            do {
                if (this.valuesToArrive == 3) this.width = this.loadedWidth;
                if (this.valuesToArrive == 2) this.height = this.loadedHeight;
                if (this.valuesToArrive == 1) this.aspectRatio = new AspectRatio(this.loadedWidth, this.loadedHeight);
                this.valuesToArrive--;
            } while ((this.valuesToArrive > 0) && (this.readsBetweenValues == 0));
            if (this.valuesToArrive > 0) this.readsUntilNextValue = this.readsBetweenValues;
        }

        @Override
        public int getWidth() {
            this.onRead();
            return super.getWidth();
        }

        @Override
        public int getHeight() {
            this.onRead();
            return super.getHeight();
        }

        @Override
        public @NotNull AspectRatio getAspectRatio() {
            this.onRead();
            return this.aspectRatio;
        }

    }

    private static class TestTexture implements ITexture {

        protected int width;
        protected int height;

        private TestTexture(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public @Nullable ResourceLocation getResourceLocation() {
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
