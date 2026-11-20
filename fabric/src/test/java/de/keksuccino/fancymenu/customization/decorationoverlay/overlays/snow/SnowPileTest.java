package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowPileTest {

    private static final CollisionAreaBounds BUTTON = new CollisionAreaBounds(165, 92, 150, 18);

    private static SnowPile elementPile(int scale) {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(BUTTON), 480, 270);
        return new SnowPile(surfaces.elements().get(0), scale, new PixelRandom(7));
    }

    private static SnowPile floorPile() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(), 480, 270);
        return new SnowPile(surfaces.floor(), 1, new PixelRandom(7));
    }

    @Test
    void elementCapsOverhangOneColumnOnEachSide() {
        SnowPile pile = elementPile(1);
        assertEquals(152, pile.columns);
        assertEquals(164, pile.x0);
        assertEquals(0, pile.columnAt(164.5F));
        assertEquals(151, pile.columnAt(315.0F));
        assertEquals(-1, pile.columnAt(163.0F));
    }

    @Test
    void elementCapsHaveRoundedEndsAndAMaximumHeight() {
        SnowPile pile = elementPile(1);
        assertEquals(0.8F, pile.limit(0), 1.0E-5F);
        assertEquals(1.6F, pile.limit(1), 1.0E-5F);
        assertEquals(SnowPile.ELEMENT_MAX_HEIGHT, pile.limit(75), 1.0E-5F);
        assertEquals(0.8F, pile.limit(pile.columns - 1), 1.0E-5F);
        for (int i = 0; i < pile.columns; i++) assertTrue(pile.heights[i] <= pile.limit(i) + 1.0E-5F, "column " + i);
        // Burying it in snow never goes past the limits
        for (int k = 0; k < 400; k++) pile.deposit(k % pile.columns, 1.2F);
        for (int i = 0; i < pile.columns; i++) assertTrue(pile.heights[i] <= pile.limit(i) + 1.0E-5F, "column " + i);
        assertEquals(SnowPile.ELEMENT_MAX_HEIGHT, pile.heights[75], 1.0E-4F);
    }

    @Test
    void theFloorIsFlatAtItsMaximumAndHasNoIcicles() {
        SnowPile pile = floorPile();
        assertEquals(480, pile.columns);
        assertEquals(0, pile.x0);
        assertEquals(SnowPile.FLOOR_MAX_HEIGHT, pile.limit(0), 1.0E-5F);
        assertEquals(0, pile.icicleX.length);
        assertEquals(270 - Math.round(pile.heights[10]), pile.topY(10));
    }

    @Test
    void aDepositSpreadsHalfOnTheColumnAndAQuarterOnEachNeighbor() {
        SnowPile pile = floorPile();
        java.util.Arrays.fill(pile.heights, 2.0F);
        pile.deposit(100, 0.8F);
        assertEquals(2.4F, pile.heights[100], 1.0E-5F);
        assertEquals(2.2F, pile.heights[99], 1.0E-5F);
        assertEquals(2.2F, pile.heights[101], 1.0E-5F);
        assertEquals(2.0F, pile.heights[98], 1.0E-5F);
    }

    @Test
    void steepStepsSlumpUntilTheyAreAboutOnePixel() {
        SnowPile pile = floorPile();
        java.util.Arrays.fill(pile.heights, 0.0F);
        pile.heights[200] = 5.0F;
        float before = sum(pile.heights);
        pile.deposit(200, 0.0F);
        // Slumping moves snow, it never adds or removes any
        assertEquals(before, sum(pile.heights), 1.0E-4F);
        assertTrue(pile.heights[200] - pile.heights[201] <= 1.1F + 1.0E-4F);
        assertTrue(pile.heights[201] > 0.0F);
    }

    @Test
    void capsMeltSlowlyAndIciclesGrowToTheirLength() {
        SnowPile pile = elementPile(1);
        java.util.Arrays.fill(pile.heights, 1.0F);
        pile.advance(10.0F);
        assertEquals(1.0F - SnowPile.MELT_PER_SECOND * 10.0F, pile.heights[50], 1.0E-5F);
        for (int k = 0; k < 200; k++) pile.advance(1.0F);
        assertEquals(0.0F, pile.heights[50], 0.0F);
        for (int k = 0; k < pile.icicleLength.length; k++) {
            assertTrue(pile.icicleLength[k] >= pile.icicleMaxLength[k]);
            assertTrue(pile.icicleLength[k] < pile.icicleMaxLength[k] + 1.0F);
            assertTrue((pile.icicleX[k] >= BUTTON.x()) && (pile.icicleX[k] < BUTTON.x() + BUTTON.width()));
        }
    }

    @Test
    void biggerSizesUseBiggerColumnsAndRows() {
        SnowPile pile = elementPile(3);
        assertEquals(50 + 2, pile.columns);
        assertEquals(162, pile.x0);
        java.util.Arrays.fill(pile.heights, 2.0F);
        assertEquals(92 - 6, pile.topY(10));
        assertEquals(1, pile.columnAt(165.0F));
    }

    @Test
    void thePileOnlyAsksToBeDrawnAgainWhenARoundedHeightChanged() {
        SnowPile pile = floorPile();
        pile.needsDraw = false;
        // Nothing drawn yet means every column counts as height 0
        java.util.Arrays.fill(pile.heights, 0.2F);
        pile.advance(0.0F);
        assertFalse(pile.needsDraw);
        pile.heights[5] = 0.6F;
        pile.advance(0.0F);
        assertTrue(pile.needsDraw);
    }

    @Test
    void capsStayOutOfTheElementRightAbove() {
        // Vanilla button columns leave only 4 pixels between two buttons
        CollisionAreaBounds upper = new CollisionAreaBounds(165, 68, 150, 20);
        CollisionAreaBounds lower = new CollisionAreaBounds(165, 92, 150, 20);
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(upper, lower), 480, 270);
        PixelSurfaces.Surface<Void> lowerSurface = surfaces.elements().get(1);
        SnowPile pile = new SnowPile(lowerSurface, 1, new PixelRandom(7));
        pile.fitBetweenElements(lowerSurface, surfaces.elements());
        for (int k = 0; k < 400; k++) pile.deposit(k % pile.columns, 1.2F);
        assertEquals(4.0F, pile.heights[75], 1.0E-4F);
        // The overhang columns reach past the upper button and keep their rounded ends
        assertEquals(0.8F, pile.limit(0), 1.0E-5F);

        SnowPile big = new SnowPile(lowerSurface, 3, new PixelRandom(7));
        big.fitBetweenElements(lowerSurface, surfaces.elements());
        for (int k = 0; k < 400; k++) big.deposit(k % big.columns, 1.2F);
        assertEquals(1.0F, big.heights[20], 1.0E-4F);
        assertTrue(big.topY(20) >= upper.y() + upper.height());
    }

    @Test
    void iciclesStayOutOfTheElementRightBelow() {
        CollisionAreaBounds upper = new CollisionAreaBounds(165, 68, 150, 20);
        CollisionAreaBounds lower = new CollisionAreaBounds(165, 92, 150, 20);
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(upper, lower), 480, 270);
        PixelSurfaces.Surface<Void> upperSurface = surfaces.elements().get(0);
        for (int scale = 1; scale <= 3; scale++) {
            SnowPile pile = new SnowPile(upperSurface, scale, new PixelRandom(7));
            pile.fitBetweenElements(upperSurface, surfaces.elements());
            for (int k = 0; k < 200; k++) pile.advance(1.0F);
            for (int k = 0; k < pile.icicleLength.length; k++) {
                assertTrue((int)pile.icicleLength[k] * scale <= 4, "scale " + scale + " icicle " + k);
            }
        }
    }

    private static float sum(float[] values) {
        float total = 0.0F;
        for (float value : values) total += value;
        return total;
    }

}
