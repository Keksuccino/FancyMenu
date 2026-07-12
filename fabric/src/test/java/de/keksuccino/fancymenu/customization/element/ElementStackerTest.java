package de.keksuccino.fancymenu.customization.element;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementStackerTest {

    private final TestElementStacker stacker = new TestElementStacker();

    @Test
    void copiesCompleteBaselineMetadataAtomicallyIncludingFractionalScale() {
        TestElement source = elementWithBaseline(1922, 1080, 2.5D, 769, 432);
        TestElement target = elementWithBaseline(800, 600, 1.0D, 800, 600);

        this.stacker.stackElementsSingleInternal(source, target);

        assertBaseline(target, 1922, 1080, 2.5D, 769, 432);
    }

    @Test
    void laterLegacyBaselineClearsPreviouslyStackedGuiScale() {
        TestElement target = new TestElement();
        this.stacker.stackElementsSingleInternal(elementWithBaseline(1920, 1080, 2.5D, 768, 432), target);
        this.stacker.stackElementsSingleInternal(elementWithBaseline(1280, 720, 0.0D, 0, 0), target);

        assertBaseline(target, 1280, 720, 0.0D, 0, 0);
    }

    @Test
    void replacesTheEntireTupleWhenEitherBaselineDimensionIsNonzero() {
        TestElement widthOnlySource = elementWithBaseline(800, 0, 1.5D, 0, 0);
        TestElement widthOnlyTarget = elementWithBaseline(1920, 1080, 2.0D, 960, 540);
        this.stacker.stackElementsSingleInternal(widthOnlySource, widthOnlyTarget);
        assertBaseline(widthOnlyTarget, 800, 0, 1.5D, 0, 0);

        TestElement heightOnlySource = elementWithBaseline(0, 600, 3.25D, 0, 0);
        TestElement heightOnlyTarget = elementWithBaseline(1920, 1080, 2.0D, 960, 540);
        this.stacker.stackElementsSingleInternal(heightOnlySource, heightOnlyTarget);
        assertBaseline(heightOnlyTarget, 0, 600, 3.25D, 0, 0);
    }

    @Test
    void logicalOnlyBaselineAlsoReplacesTheEntireTuple() {
        TestElement source = elementWithBaseline(0, 0, 0.75D, 5363, 2676);
        TestElement target = elementWithBaseline(1920, 1080, 2.0D, 960, 540);

        this.stacker.stackElementsSingleInternal(source, target);

        assertBaseline(target, 0, 0, 0.75D, 5363, 2676);
    }

    private static TestElement elementWithBaseline(int width, int height, double guiScale, int guiWidth, int guiHeight) {
        TestElement element = new TestElement();
        element.autoSizingBaseScreenWidth = width;
        element.autoSizingBaseScreenHeight = height;
        element.autoSizingBaseGuiScale = guiScale;
        element.autoSizingBaseGuiWidth = guiWidth;
        element.autoSizingBaseGuiHeight = guiHeight;
        return element;
    }

    private static void assertBaseline(TestElement element, int width, int height, double guiScale, int guiWidth, int guiHeight) {
        assertEquals(width, element.autoSizingBaseScreenWidth);
        assertEquals(height, element.autoSizingBaseScreenHeight);
        assertEquals(guiScale, element.autoSizingBaseGuiScale);
        assertEquals(guiWidth, element.autoSizingBaseGuiWidth);
        assertEquals(guiHeight, element.autoSizingBaseGuiHeight);
    }

    private static final class TestElement extends AbstractElement {

        private TestElement() {
            super(null);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

    }

    private static final class TestElementStacker implements ElementStacker<TestElement> {

        @Override
        public void stackElements(@NotNull TestElement element, @NotNull TestElement stack) {
        }

    }

}
