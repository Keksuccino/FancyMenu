package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelSurfacesTest {

    private static final CollisionAreaBounds BUTTON_A = new CollisionAreaBounds(165, 92, 150, 18);
    private static final CollisionAreaBounds BUTTON_B = new CollisionAreaBounds(165, 116, 150, 18);

    @Test
    void elementsComeFirstAndTheFloorLast() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        assertTrue(surfaces.update(List.of(BUTTON_A, BUTTON_B), 480, 270));

        assertEquals(3, surfaces.all().size());
        assertEquals(2, surfaces.elements().size());
        PixelSurfaces.Surface<Void> floor = surfaces.floor();
        assertSame(floor, surfaces.all().get(surfaces.all().size() - 1));
        assertTrue(floor.isFloor());
        assertEquals(480, floor.width());
        assertEquals(269, floor.restY());
        assertEquals(91, surfaces.elements().get(0).restY());
        assertEquals(480 + 150 + 150, surfaces.totalWidth());
    }

    @Test
    void statesSurviveUpdatesWithTheSameBounds() {
        AtomicInteger created = new AtomicInteger();
        PixelSurfaces<String> surfaces = new PixelSurfaces<>(surface -> "state " + created.incrementAndGet());
        surfaces.update(List.of(BUTTON_A, BUTTON_B), 480, 270);
        PixelSurfaces.Surface<String> first = surfaces.elements().get(0);
        String state = first.state();
        int version = surfaces.version();

        // A new list with the same boxes, like every screen tick
        assertFalse(surfaces.update(new ArrayList<>(List.of(BUTTON_A, BUTTON_B)), 480, 270));
        assertSame(first, surfaces.elements().get(0));
        assertEquals(state, first.state());
        assertEquals(version, surfaces.version());
        assertEquals(3, created.get());

        // Button B moved: A keeps its state, B gets a new one
        assertTrue(surfaces.update(List.of(BUTTON_A, new CollisionAreaBounds(165, 140, 150, 18)), 480, 270));
        assertSame(first, surfaces.elements().get(0));
        assertEquals(4, created.get());
        assertNotEquals(version, surfaces.version());
    }

    @Test
    void aNewScreenSizeGivesANewFloor() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(BUTTON_A), 480, 270);
        PixelSurfaces.Surface<Void> floor = surfaces.floor();
        assertTrue(surfaces.update(List.of(BUTTON_A), 640, 360));
        assertNotSame(floor, surfaces.floor());
        assertEquals(640, surfaces.floor().width());
        assertEquals(359, surfaces.floor().restY());
    }

    @Test
    void picksAreWeightedByWidth() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(BUTTON_A, BUTTON_B), 480, 270);
        PixelRandom random = new PixelRandom(3);
        int floor = 0;
        int buttonA = 0;
        int rounds = 78_000;
        for (int i = 0; i < rounds; i++) {
            PixelSurfaces.Surface<Void> picked = surfaces.pick(random);
            if (picked.isFloor()) floor++;
            else if (picked == surfaces.elements().get(0)) buttonA++;
        }
        // 480 of 780 pixels are floor, 150 belong to each button
        assertEquals(480.0 / 780.0, floor / (double)rounds, 0.01);
        assertEquals(150.0 / 780.0, buttonA / (double)rounds, 0.01);
    }

    @Test
    void landingFindsTheFirstSurfaceWhoseRestRowWasCrossed() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(BUTTON_A, BUTTON_B), 480, 270);

        assertSame(surfaces.elements().get(0), surfaces.findLanding(200.0F, 89.5F, 91.0F));
        // Still above the rest row, already below it, or next to the buttons
        assertNull(surfaces.findLanding(200.0F, 80.0F, 90.9F));
        assertNull(surfaces.findLanding(200.0F, 91.0F, 93.0F));
        assertNull(surfaces.findLanding(100.0F, 80.0F, 100.0F));
        assertSame(surfaces.floor(), surfaces.findLanding(100.0F, 268.5F, 270.0F));
        assertSame(surfaces.elements().get(1), surfaces.findElementAt(170.0F, 120.0F));
        assertNull(surfaces.findElementAt(170.0F, 110.0F));
    }

}
