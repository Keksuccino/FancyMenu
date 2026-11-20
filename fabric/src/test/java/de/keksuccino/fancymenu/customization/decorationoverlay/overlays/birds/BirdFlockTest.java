package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.birds;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureTestScene;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdFlockTest {

    @Test
    void twoThirdsOfTheBirdsAlreadySitSomewhereAtTheStart() {
        CreatureTestScene test = new CreatureTestScene(3, CreatureTestScene.MOCKUP_BUTTONS);
        BirdFlock flock = new BirdFlock(test.scene);
        test.frame(flock, CreatureTestScene.FRAME);
        assertEquals(6, flock.birdCount());
        assertEquals(4, count(flock, BirdFlock.PERCHED));
        assertEquals(2, count(flock, BirdFlock.AWAY));
    }

    @Test
    void theAmountSetsHowManyBirdsVisit() {
        CreatureTestScene test = new CreatureTestScene(5, CreatureTestScene.MOCKUP_BUTTONS);
        BirdFlock flock = new BirdFlock(test.scene);
        test.run(flock, 20.0F);
        assertEquals(6, flock.birdCount());

        // New birds fly in
        test.setAmount(3.0F);
        test.frame(flock, CreatureTestScene.FRAME);
        assertEquals(18, flock.birdCount());
        test.run(flock, 20.0F);
        assertTrue(count(flock, BirdFlock.PERCHED) > 6, "more birds should sit around at 300%");

        // Surplus ones fly off and are gone once they left the screen
        test.setAmount(0.25F);
        test.frame(flock, CreatureTestScene.FRAME);
        for (int i = 2; i < flock.birdCount(); i++) assertTrue(flock.bird(i).state != BirdFlock.PERCHED, "surplus birds have to fly off");
        test.run(flock, 20.0F);
        assertEquals(2, flock.birdCount());

        // Bigger screens get more birds
        test.setAmount(1.0F);
        test.setSize(960, 540);
        test.frame(flock, CreatureTestScene.FRAME);
        assertEquals(24, flock.birdCount());
    }

    @Test
    void thePointerStartlesABirdAndTheBirdsNextToIt() {
        CreatureTestScene test = new CreatureTestScene(11, CreatureTestScene.MOCKUP_BUTTONS);
        test.setAmount(3.0F);
        BirdFlock flock = new BirdFlock(test.scene);
        test.run(flock, 15.0F);
        BirdFlock.Bird target = null;
        for (int i = 0; i < flock.birdCount(); i++) {
            BirdFlock.Bird b = flock.bird(i);
            if ((b.state == BirdFlock.PERCHED) && (b.act == BirdFlock.ACT_NONE)) target = b;
        }
        assertNotNull(target);
        float shy = 28.0F + 8.0F;
        List<BirdFlock.Bird> neighbors = new ArrayList<>();
        List<BirdFlock.Bird> farAway = new ArrayList<>();
        for (int i = 0; i < flock.birdCount(); i++) {
            BirdFlock.Bird b = flock.bird(i);
            if ((b == target) || (b.state != BirdFlock.PERCHED)) continue;
            // At size 1 the distance to the pointer is the same as the distance to the startled bird
            float distance = (float)Math.hypot(b.x - target.x, b.y - target.y);
            if (distance < shy + 10.0F) neighbors.add(b);
            else if ((distance > 2.0F * shy) && (b.stay > 1.0F)) farAway.add(b);
        }
        test.movePointer(target.x, target.y - 3.0F);
        test.frame(flock, CreatureTestScene.FRAME);
        assertEquals(BirdFlock.FLEEING, target.state);
        // It flies off upwards
        assertTrue(target.vy < 0.0F);
        for (BirdFlock.Bird b : neighbors) assertEquals(BirdFlock.FLEEING, b.state, "a neighbor stayed");
        for (BirdFlock.Bird b : farAway) assertEquals(BirdFlock.PERCHED, b.state, "a bird far away got startled");
    }

    @Test
    void perchedBirdsStandOnTheirSurfaceAndHopsNeverLeaveIt() {
        for (int scale = 1; scale <= 3; scale++) {
            CreatureTestScene test = new CreatureTestScene(17 + scale, CreatureTestScene.MOCKUP_BUTTONS);
            test.setScale(scale);
            test.setAmount(3.0F);
            BirdFlock flock = new BirdFlock(test.scene);
            for (int frame = 0; frame < 60 * 60; frame++) {
                test.frame(flock, CreatureTestScene.FRAME);
                for (int i = 0; i < flock.birdCount(); i++) {
                    BirdFlock.Bird b = flock.bird(i);
                    if (b.state != BirdFlock.PERCHED) {
                        assertFalse(BirdFlock.isElementContent(b), "flying birds are front content");
                        continue;
                    }
                    PixelSurfaces.Surface<?> surface = b.surface;
                    assertNotNull(surface);
                    assertEquals(!surface.isFloor(), BirdFlock.isElementContent(b), "only birds perched on an element are element content");
                    assertEquals(surface.restY(), Math.round(b.y));
                    boolean hopping = b.act == BirdFlock.ACT_HOP;
                    float from = hopping ? Math.min(b.x, b.hopToX) : b.x;
                    float to = hopping ? Math.max(b.x, b.hopToX) : b.x;
                    assertTrue((from >= surface.x() + 4 * scale) && (to <= surface.right() - 1 - 4 * scale), "bird at " + b.x + " left its surface at size " + scale);
                }
            }
        }
    }

    @Test
    void withoutElementsBirdsLandOnTheFloor() {
        CreatureTestScene test = new CreatureTestScene(23, List.of());
        BirdFlock flock = new BirdFlock(test.scene);
        test.run(flock, 30.0F);
        int perched = 0;
        for (int i = 0; i < flock.birdCount(); i++) {
            BirdFlock.Bird b = flock.bird(i);
            if (b.state != BirdFlock.PERCHED) continue;
            perched++;
            assertTrue(b.surface.isFloor());
            assertFalse(BirdFlock.isElementContent(b), "birds on the floor are screen-wide content");
            assertEquals(269, Math.round(b.y));
        }
        assertTrue(perched > 0);
    }

    @Test
    void birdsLeaveElementsThatGoAway() {
        CreatureTestScene test = new CreatureTestScene(29, CreatureTestScene.MOCKUP_BUTTONS);
        test.setAmount(3.0F);
        BirdFlock flock = new BirdFlock(test.scene);
        test.run(flock, 10.0F);
        assertTrue(countOnElements(flock) > 0);
        test.setBoxes(List.of());
        test.frame(flock, CreatureTestScene.FRAME);
        assertEquals(0, countOnElements(flock));
    }

    private static int countOnElements(BirdFlock flock) {
        int onElements = 0;
        for (int i = 0; i < flock.birdCount(); i++) {
            BirdFlock.Bird b = flock.bird(i);
            if (((b.state == BirdFlock.PERCHED) || (b.state == BirdFlock.ARRIVING)) && !b.surface.isFloor()) onElements++;
        }
        return onElements;
    }

    private static int count(BirdFlock flock, int state) {
        int count = 0;
        for (int i = 0; i < flock.birdCount(); i++) {
            if (flock.bird(i).state == state) count++;
        }
        return count;
    }

}
