package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfettiLitterTest {

    private static final CollisionAreaBounds BUTTON = new CollisionAreaBounds(165, 92, 150, 18);

    private static PixelSurfaces<ConfettiSurface> surfaces() {
        PixelSurfaces<ConfettiSurface> surfaces = new PixelSurfaces<>(ConfettiSurface::new);
        surfaces.update(List.of(BUTTON), 480, 270);
        return surfaces;
    }

    @Test
    void piecesLieFlatOnTopOfEachOtherUpToTheLayerLimit() {
        ConfettiSurface button = surfaces().elements().get(0).state();
        ConfettiLitter litter = new ConfettiLitter(260);
        for (int i = 0; i < ConfettiSurface.ELEMENT_MAX_LEVELS; i++) assertTrue(litter.settle(4, 0, button, 200.0F, 1));
        assertFalse(litter.settle(4, 0, button, 200.0F, 1));
        assertEquals(91, litter.pieces.get(0).y);
        assertEquals(90, litter.pieces.get(1).y);
        assertEquals(89, litter.pieces.get(2).y);
        assertEquals(3, button.levels[200 - 165]);
        assertEquals(0, button.levels[204 - 165]);
    }

    @Test
    void theFloorHoldsMoreLayersAndPiecesStayInsideTheirSurface() {
        PixelSurfaces<ConfettiSurface> surfaces = surfaces();
        ConfettiSurface floor = surfaces.floor().state();
        ConfettiSurface button = surfaces.elements().get(0).state();
        ConfettiLitter litter = new ConfettiLitter(260);
        for (int i = 0; i < ConfettiSurface.FLOOR_MAX_LEVELS; i++) assertTrue(litter.settle(2, 0, floor, 10.0F, 1));
        assertFalse(litter.settle(2, 0, floor, 10.0F, 1));
        assertEquals(269 - 4, litter.pieces.get(4).y);
        assertTrue(litter.settle(6, 0, button, 320.0F, 1));
        assertEquals(165 + 150 - 6, litter.pieces.get(litter.pieces.size() - 1).x);
    }

    @Test
    void tooMuchLitterDissolvesTheOldestAndThePiecesAboveSinkDown() {
        ConfettiSurface button = surfaces().elements().get(0).state();
        ConfettiLitter litter = new ConfettiLitter(2);
        litter.settle(4, 0, button, 200.0F, 1);
        litter.settle(4, 1, button, 200.0F, 1);
        litter.settle(4, 2, button, 200.0F, 1);
        ConfettiLitter.Piece oldest = litter.pieces.get(0);
        ConfettiLitter.Piece top = litter.pieces.get(litter.pieces.size() - 1);
        assertEquals(1, oldest.blinkStep());
        litter.update(0.3F, 1);
        assertEquals(3, litter.size());
        litter.update(0.3F, 1);
        assertEquals(2, litter.size());
        assertFalse(litter.pieces.contains(oldest));
        assertEquals(2, top.level);
        assertEquals(90, top.y);
        assertEquals(2, button.levels[200 - 165]);
        assertEquals(0, top.blinkStep());
    }

    @Test
    void aLoweredCapDissolvesTheExtraPiecesOneByOne() {
        ConfettiSurface floor = surfaces().floor().state();
        ConfettiLitter litter = new ConfettiLitter(10);
        for (int i = 0; i < 6; i++) litter.settle(3, 0, floor, i * 10.0F, 1);
        litter.cap = 3;
        for (int frame = 0; frame < 60; frame++) litter.update(0.1F, 1);
        assertEquals(3, litter.size());
    }

    @Test
    void litterOfSurfacesThatAreGoneGetsDropped() {
        PixelSurfaces<ConfettiSurface> surfaces = surfaces();
        ConfettiSurface floor = surfaces.floor().state();
        ConfettiSurface button = surfaces.elements().get(0).state();
        ConfettiLitter litter = new ConfettiLitter(260);
        litter.settle(3, 0, floor, 10.0F, 1);
        litter.settle(3, 0, button, 200.0F, 1);
        floor.seenInVersion = 7;
        litter.dropUnseen(7);
        assertEquals(1, litter.size());
        assertEquals(floor, litter.pieces.get(0).surface);
    }

    @Test
    void litterStaysOutOfTheElementRightAbove() {
        CollisionAreaBounds upper = new CollisionAreaBounds(165, 68, 150, 20);
        CollisionAreaBounds lower = new CollisionAreaBounds(165, 90, 150, 20);
        PixelSurfaces<ConfettiSurface> surfaces = new PixelSurfaces<>(ConfettiSurface::new);
        surfaces.update(List.of(upper, lower), 480, 270);
        PixelSurfaces.Surface<ConfettiSurface> lowerSurface = surfaces.elements().get(1);
        ConfettiSurface state = lowerSurface.state();
        state.fitUnder(lowerSurface, surfaces.elements());
        ConfettiLitter litter = new ConfettiLitter(260);
        // Two free rows: two thin layers, or no layer at all of pieces three pixels thick
        assertTrue(litter.settle(4, 0, state, 200.0F, 1));
        assertTrue(litter.settle(4, 0, state, 200.0F, 1));
        assertFalse(litter.settle(4, 0, state, 200.0F, 1));
        assertFalse(litter.settle(4, 0, state, 250.0F, 3));
    }

    @Test
    void biggerSizesStackThickerPieces() {
        ConfettiSurface button = surfaces().elements().get(0).state();
        ConfettiLitter litter = new ConfettiLitter(260);
        litter.settle(4, 0, button, 200.0F, 3);
        litter.settle(4, 0, button, 200.0F, 3);
        assertEquals(12, litter.pieces.get(0).width);
        assertEquals(92 - 3, litter.pieces.get(0).y);
        assertEquals(92 - 6, litter.pieces.get(1).y);
    }

}
