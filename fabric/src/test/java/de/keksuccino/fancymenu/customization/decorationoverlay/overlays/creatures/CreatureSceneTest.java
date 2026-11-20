package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureSceneTest {

    @Test
    void densityFollowsTheScreenAreaWithinItsLimits() {
        assertEquals(1.0F, CreatureScene.densityOf(480, 270), 1.0E-6F);
        assertEquals(4.0F, CreatureScene.densityOf(960, 540), 1.0E-6F);
        assertEquals(CreatureScene.MAX_DENSITY, CreatureScene.densityOf(3840, 2160));
        assertEquals(CreatureScene.MIN_DENSITY, CreatureScene.densityOf(100, 60));

        CreatureTestScene test = new CreatureTestScene(1, List.of());
        test.setSize(960, 540);
        test.run(new IdleSimulation(test.scene), CreatureTestScene.FRAME);
        assertEquals(4.0F, test.scene.density(), 1.0E-6F);
        assertEquals(2.0F, test.scene.eventRate(), 1.0E-6F);
        assertEquals(540.0F * 110.0F / 270.0F, test.scene.stageY(110.0F), 1.0E-4F);
    }

    @Test
    void spotsStayBetweenTheInsetsOfTheirSurface() {
        CreatureScene scene = scene(List.of(new CollisionAreaBounds(165, 92, 150, 18)));
        CreatureScene.Spot spot = new CreatureScene.Spot();
        int found = 0;
        for (int i = 0; i < 2000; i++) {
            if (!scene.pickSpot(10.0F, 14.0F, 6, spot)) continue;
            found++;
            PixelSurfaces.Surface<?> surface = spot.surface;
            assertTrue(surface != null);
            assertEquals(surface.restY(), spot.y);
            assertTrue((spot.x >= surface.x() + 10) && (spot.x <= surface.right() - 14), "column " + spot.x + " is outside of the insets");
        }
        assertEquals(2000, found);
    }

    @Test
    void surfacesTooNarrowForTheInsetsGetNoSpots() {
        CreatureScene scene = scene(List.of(new CollisionAreaBounds(100, 100, 12, 20)));
        CreatureScene.Spot spot = new CreatureScene.Spot();
        for (int i = 0; i < 500; i++) {
            if (scene.pickSpot(8.0F, 8.0F, 6, spot)) assertTrue(spot.surface.isFloor());
        }
    }

    @Test
    void spotsCoveredByAnotherElementOrAboveTheScreenGetRejected() {
        CollisionAreaBounds covered = new CollisionAreaBounds(100, 100, 200, 20);
        CollisionAreaBounds cover = new CollisionAreaBounds(0, 90, 480, 15);
        CollisionAreaBounds atTheTop = new CollisionAreaBounds(0, 3, 480, 10);
        CreatureScene scene = scene(List.of(covered, cover, atTheTop));
        CreatureScene.Spot spot = new CreatureScene.Spot();
        boolean sawFloor = false;
        for (int i = 0; i < 2000; i++) {
            if (!scene.pickSpot(4.0F, 4.0F, 6, spot)) continue;
            assertFalse(spot.surface.y() == 100, "the covered element got a spot");
            assertFalse(spot.surface.y() == 3, "a creature would stick out of the top of the screen");
            sawFloor |= spot.surface.isFloor();
        }
        assertTrue(sawFloor);
    }

    @Test
    void surfacesThatMovedAreNotLiveAnymore() {
        CreatureTestScene test = new CreatureTestScene(1, List.of(new CollisionAreaBounds(10, 10, 50, 10)));
        IdleSimulation idle = new IdleSimulation(test.scene);
        test.run(idle, CreatureTestScene.FRAME);
        assertTrue(test.scene.surfacesChanged());
        PixelSurfaces.Surface<?> before = test.surfaces.elements().get(0);
        assertTrue(test.scene.isSurfaceLive(before));

        test.run(idle, CreatureTestScene.FRAME);
        assertFalse(test.scene.surfacesChanged());

        test.setBoxes(List.of(new CollisionAreaBounds(12, 10, 50, 10)));
        test.run(idle, CreatureTestScene.FRAME);
        assertTrue(test.scene.surfacesChanged());
        assertNotSame(before, test.surfaces.elements().get(0));
        assertFalse(test.scene.isSurfaceLive(before));
        assertFalse(test.scene.isSurfaceLive(null));
    }

    @Test
    void jitterWandersTheSameAmountPerSecondAtAnyFrameRate() {
        double at60 = jitterVariancePerSecond(1.0F / 60.0F);
        double at240 = jitterVariancePerSecond(1.0F / 240.0F);
        double at20 = jitterVariancePerSecond(1.0F / 20.0F);
        // The mockup's variance per second: 60 steps of uniform(-1, 1) * 3 / 60
        double mockup = 60.0 * (3.0 / 60.0) * (3.0 / 60.0) / 3.0;
        assertEquals(mockup, at60, mockup * 0.1);
        assertEquals(mockup, at240, mockup * 0.1);
        assertEquals(mockup, at20, mockup * 0.1);
    }

    private static double jitterVariancePerSecond(float dt) {
        CreatureScene scene = scene(List.of());
        int steps = Math.round(1.0F / dt);
        double sumOfSquares = 0.0;
        int trials = 4000;
        for (int t = 0; t < trials; t++) {
            double sum = 0.0;
            for (int i = 0; i < steps; i++) sum += scene.jitter(3.0F, dt);
            sumOfSquares += sum * sum;
        }
        return sumOfSquares / trials;
    }

    @Test
    void poolsReuseRemovedObjectsAndKeepTheOrder() {
        CreaturePool<int[]> pool = new CreaturePool<>(() -> new int[1]);
        List<int[]> created = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int[] object = pool.add();
            object[0] = i;
            created.add(object);
        }
        pool.remove(1);
        assertEquals(4, pool.size());
        assertEquals(List.of(0, 2, 3, 4), values(pool));
        assertSame(created.get(1), pool.add());

        pool.truncate(2);
        assertEquals(List.of(0, 2), values(pool));
        int[] reused = pool.add();
        assertTrue(created.contains(reused));
        pool.clear();
        assertEquals(0, pool.size());
    }

    private static List<Integer> values(CreaturePool<int[]> pool) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) values.add(pool.get(i)[0]);
        return values;
    }

    private static CreatureScene scene(List<CollisionAreaBounds> boxes) {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(boxes, 480, 270);
        CreatureScene scene = new CreatureScene(new PixelRandom(42), new PixelPointer(), surfaces);
        scene.update(480, 270, 0.0F, 1.0F, 1);
        return scene;
    }

    private static final class IdleSimulation extends CreatureSimulation {

        private IdleSimulation(CreatureScene scene) {
            super(scene);
        }

        @Override
        public void update(float deltaSeconds) {
        }

    }

}
