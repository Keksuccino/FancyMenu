package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.butterflies;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureTestScene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButterflySwarmTest {

    @Test
    void theAmountAndTheScreenSetHowManyButterfliesThereAre() {
        CreatureTestScene test = new CreatureTestScene(2, CreatureTestScene.MOCKUP_BUTTONS);
        ButterflySwarm swarm = new ButterflySwarm(test.scene);
        test.frame(swarm, CreatureTestScene.FRAME);
        assertEquals(9, swarm.nearCount());
        assertEquals(7, swarm.farCount());

        test.setAmount(3.0F);
        test.frame(swarm, CreatureTestScene.FRAME);
        assertEquals(27, swarm.nearCount());
        assertEquals(21, swarm.farCount());

        test.setAmount(0.25F);
        test.frame(swarm, CreatureTestScene.FRAME);
        assertEquals(2, swarm.nearCount());
        assertEquals(2, swarm.farCount());

        test.setAmount(1.0F);
        test.setSize(960, 540);
        test.frame(swarm, CreatureTestScene.FRAME);
        assertEquals(36, swarm.nearCount());
        assertEquals(28, swarm.farCount());
    }

    @Test
    void restingButterfliesSitOnTopOfTheirSurfaceAndTakeOffWhenThePointerComesClose() {
        for (int scale = 1; scale <= 3; scale++) {
            CreatureTestScene test = new CreatureTestScene(31 + scale, CreatureTestScene.MOCKUP_BUTTONS);
            test.setScale(scale);
            test.setAmount(3.0F);
            ButterflySwarm swarm = new ButterflySwarm(test.scene);
            ButterflySwarm.Butterfly resting = null;
            for (int frame = 0; (frame < 120 * 60) && (resting == null); frame++) {
                test.frame(swarm, CreatureTestScene.FRAME);
                for (int i = 0; i < swarm.nearCount(); i++) {
                    ButterflySwarm.Butterfly f = swarm.nearButterfly(i);
                    if (f.state != ButterflySwarm.RESTING) {
                        assertFalse(ButterflySwarm.isElementContent(f), "flying butterflies are front content");
                        continue;
                    }
                    assertNotNull(f.surface);
                    assertEquals(!f.surface.isFloor(), ButterflySwarm.isElementContent(f), "only butterflies resting on an element are element content");
                    assertEquals(f.surface.restY(), f.targetY);
                    assertEquals(f.targetY - ButterflySwarm.restOffset(scale), f.y, 1.0E-4F);
                    if (f.restDuration - f.restTime > 0.5F) resting = f;
                }
            }
            assertNotNull(resting, "no butterfly came to rest at size " + scale);
            float shy = 38.0F + 8.0F * scale;
            // Close, but not close enough yet
            test.movePointer(resting.x + shy + 2.0F, resting.y + 2.0F * scale);
            test.frame(swarm, CreatureTestScene.FRAME);
            assertEquals(ButterflySwarm.RESTING, resting.state);
            test.movePointer(resting.x + shy - 2.0F, resting.y + 2.0F * scale);
            test.frame(swarm, CreatureTestScene.FRAME);
            assertEquals(ButterflySwarm.FLYING, resting.state);
        }
    }

    @Test
    void butterfliesRestingOnTheFloorAreScreenWideContent() {
        CreatureTestScene test = new CreatureTestScene(37, List.of());
        test.setAmount(3.0F);
        ButterflySwarm swarm = new ButterflySwarm(test.scene);
        int restingOnTheFloor = 0;
        for (int frame = 0; frame < 120 * 60; frame++) {
            test.frame(swarm, CreatureTestScene.FRAME);
            for (int i = 0; i < swarm.nearCount(); i++) {
                ButterflySwarm.Butterfly f = swarm.nearButterfly(i);
                assertFalse(ButterflySwarm.isElementContent(f));
                if (f.state == ButterflySwarm.RESTING) {
                    assertTrue(f.surface.isFloor());
                    restingOnTheFloor++;
                }
            }
        }
        assertTrue(restingOnTheFloor > 0, "no butterfly rested on the floor");
    }

    @Test
    void flyingButterfliesShyAwayFromThePointer() {
        CreatureTestScene test = new CreatureTestScene(41, List.of());
        ButterflySwarm swarm = new ButterflySwarm(test.scene);
        test.run(swarm, 2.0F);
        ButterflySwarm.Butterfly f = swarm.nearButterfly(0);
        f.state = ButterflySwarm.FLYING;
        f.x = 240.0F;
        f.y = 135.0F;
        f.heading = 0.0F;
        // The pointer sits right in front of it
        test.movePointer(255.0F, 137.0F);
        float before = (float)Math.hypot(f.x - 255.0F, f.y + 2.0F - 137.0F);
        test.run(swarm, 0.5F);
        float after = (float)Math.hypot(f.x - 255.0F, f.y + 2.0F - 137.0F);
        assertTrue(after > before + 5.0F, "the butterfly did not get away from the pointer: " + before + " -> " + after);
    }

    @Test
    void butterfliesLeaveElementsThatGoAway() {
        CreatureTestScene test = new CreatureTestScene(43, CreatureTestScene.MOCKUP_BUTTONS);
        test.setAmount(3.0F);
        ButterflySwarm swarm = new ButterflySwarm(test.scene);
        boolean sawOneOnAnElement = false;
        for (int frame = 0; (frame < 120 * 60) && !sawOneOnAnElement; frame++) {
            test.frame(swarm, CreatureTestScene.FRAME);
            sawOneOnAnElement = countOnElements(swarm) > 0;
        }
        assertTrue(sawOneOnAnElement);
        test.setBoxes(List.of());
        test.frame(swarm, CreatureTestScene.FRAME);
        assertEquals(0, countOnElements(swarm));
    }

    private static int countOnElements(ButterflySwarm swarm) {
        int count = 0;
        for (int i = 0; i < swarm.nearCount(); i++) {
            ButterflySwarm.Butterfly f = swarm.nearButterfly(i);
            if ((f.state != ButterflySwarm.FLYING) && (f.surface != null) && !f.surface.isFloor()) count++;
        }
        return count;
    }

}
