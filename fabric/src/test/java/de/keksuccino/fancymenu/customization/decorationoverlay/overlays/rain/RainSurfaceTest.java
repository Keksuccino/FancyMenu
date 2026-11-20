package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RainSurfaceTest {

    private static final CollisionAreaBounds UPPER = new CollisionAreaBounds(165, 68, 150, 20);
    private static final CollisionAreaBounds LOWER = new CollisionAreaBounds(165, 92, 150, 20);

    @Test
    void elementsGetDripsAtBothEndsAndInTheMiddleAndTheFloorGetsNone() {
        PixelSurfaces<RainSurface> surfaces = new PixelSurfaces<>(surface -> new RainSurface(surface, new PixelRandom(3)));
        surfaces.update(List.of(UPPER, new CollisionAreaBounds(10, 10, 30, 10)), 480, 270);
        RainSurface button = surfaces.elements().get(0).state();
        assertEquals(3, button.drips.length);
        assertEquals(166, button.dripX(button.drips[0], 1));
        assertEquals(165 + 150 - 2, button.dripX(button.drips[2], 1));
        assertEquals(2, surfaces.elements().get(1).state().drips.length);
        assertEquals(0, surfaces.floor().state().drips.length);
        // 12 glints on the 480 pixel floor of the mockup stage, 4 on a 150 pixel button
        assertEquals(12, surfaces.floor().state().glintX.length);
        assertEquals(4, button.glintX.length);
    }

    @Test
    void dripsNeverHangIntoTheElementBelowAndStillLandOnIt() {
        PixelSurfaces<RainSurface> surfaces = new PixelSurfaces<>(surface -> new RainSurface(surface, new PixelRandom(3)));
        surfaces.update(List.of(UPPER, LOWER), 480, 270);
        PixelSurfaces.Surface<RainSurface> upper = surfaces.elements().get(0);
        for (int scale = 1; scale <= 3; scale++) {
            upper.state().fitDrips(upper, surfaces.elements(), scale);
            for (RainSurface.Drip drip : upper.state().drips) {
                drip.progress = 0.9F;
                assertTrue(drip.stage(scale) * scale <= 4, "scale " + scale);
                // The fall starts above the row a drip lands on when it reaches the lower button
                assertTrue(88 + drip.fallStartOffset(scale) < 92 - scale || drip.maxStage(scale) == 0, "scale " + scale);
            }
        }
        // The mockup's gap of the stage lets drips hang three pixels and fall from two pixels under the element
        upper.state().fitDrips(upper, surfaces.elements(), 1);
        assertEquals(3, upper.state().drips[0].maxStage(1));
        assertEquals(2, upper.state().drips[0].fallStartOffset(1));
    }

}
