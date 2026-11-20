package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurfaceHeadroomTest {

    private static final CollisionAreaBounds TITLE = new CollisionAreaBounds(140, 20, 200, 40);
    private static final CollisionAreaBounds BUTTON = new CollisionAreaBounds(165, 92, 150, 20);
    private static final CollisionAreaBounds OVERLAPPING = new CollisionAreaBounds(300, 85, 30, 30);
    private static final CollisionAreaBounds TEXT = new CollisionAreaBounds(2, 260, 100, 9);

    @Test
    void theLowestElementAboveTheColumnsCounts() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(TITLE, BUTTON, OVERLAPPING, TEXT), 480, 270);
        PixelSurfaces.Surface<Void> button = surfaces.elements().get(1);
        assertEquals(92 - 60, SurfaceHeadroom.freeRowsAbove(button, 200, 10, surfaces.elements()));
        // Only the title is above these columns; the element overlapping the button's top edge doesn't count
        assertEquals(92 - 60, SurfaceHeadroom.freeRowsAbove(button, 305, 10, surfaces.elements()));
        // Nothing above: the room goes up to the top of the screen
        assertEquals(92, SurfaceHeadroom.freeRowsAbove(button, 120, 10, surfaces.elements()));
        PixelSurfaces.Surface<Void> floor = surfaces.floor();
        assertEquals(1, SurfaceHeadroom.freeRowsAbove(floor, 50, 1, surfaces.elements()));
        assertEquals(270 - 112, SurfaceHeadroom.freeRowsAbove(floor, 200, 1, surfaces.elements()));
    }

}
