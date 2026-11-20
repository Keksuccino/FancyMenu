package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureTestScene;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatColonyTest {

    @Test
    void theMockupButtonsGetTheMockupRoosts() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(CreatureTestScene.MOCKUP_BUTTONS, 480, 270);
        BatRoosts roosts = new BatRoosts();
        for (int scale = 1; scale <= 3; scale++) {
            roosts.layout(surfaces, 480, 270, scale);
            assertEquals(6, roosts.count());
            // [1, 28], [3, -34], [0, -52], [2, 46], [3, 18], [1, -24] of the mockup
            int[] buttons = {1, 3, 0, 2, 3, 1};
            int[] xs = {193, 281, 263, 211, 183, 291};
            for (int k = 0; k < 6; k++) {
                assertEquals(xs[k], roosts.x(k), "roost " + k + " at size " + scale);
                assertEquals(surfaces.elements().get(buttons[k]), roosts.surface(k));
            }
        }
    }

    @Test
    void roostsSpreadOverAnyLayoutAndSkipSpotsThatDoNotWork() {
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        BatRoosts roosts = new BatRoosts();

        // One wide element gets all six spots, a bat width apart
        surfaces.update(List.of(new CollisionAreaBounds(40, 50, 400, 20)), 480, 270);
        roosts.layout(surfaces, 480, 270, 3);
        assertEquals(6, roosts.count());
        for (int a = 0; a < 6; a++) {
            for (int b = a + 1; b < 6; b++) assertTrue(Math.abs(roosts.x(a) - roosts.x(b)) >= 17);
        }

        // No elements, elements too narrow for a bat, elements without room below and elements with another one right below get none
        surfaces.update(List.of(), 480, 270);
        roosts.layout(surfaces, 480, 270, 1);
        assertEquals(0, roosts.count());
        surfaces.update(List.of(new CollisionAreaBounds(100, 50, 4, 20), new CollisionAreaBounds(100, 260, 200, 8)), 480, 270);
        roosts.layout(surfaces, 480, 270, 1);
        assertEquals(0, roosts.count());
        surfaces.update(List.of(new CollisionAreaBounds(100, 50, 200, 20), new CollisionAreaBounds(100, 70, 200, 20)), 480, 270);
        roosts.layout(surfaces, 480, 270, 1);
        for (int k = 0; k < roosts.count(); k++) assertEquals(90, roosts.surface(k).bottom(), "a bat hangs between two stacked elements");

        // Bats always hang completely under their element, also at the ends of it and on screens that cut it off
        surfaces.update(List.of(new CollisionAreaBounds(-30, 50, 60, 20), new CollisionAreaBounds(460, 80, 60, 20)), 480, 270);
        for (int scale = 1; scale <= 3; scale++) {
            roosts.layout(surfaces, 480, 270, scale);
            assertTrue(roosts.count() > 0);
            int offset = (5 * (scale - 1)) >> 1;
            for (int k = 0; k < roosts.count(); k++) {
                PixelSurfaces.Surface<?> surface = roosts.surface(k);
                int left = roosts.x(k) - offset;
                assertTrue(left >= Math.max(0, surface.x()), "bat sticks out on the left");
                assertTrue(left + 5 * scale <= Math.min(480, surface.right()), "bat sticks out on the right");
            }
        }
    }

    @Test
    void theAmountSetsHowManyBatsHang() {
        assertEquals(1, BatColony.hangingCount(0.25F));
        assertEquals(1, BatColony.hangingCount(0.5F));
        assertEquals(2, BatColony.hangingCount(1.0F));
        assertEquals(3, BatColony.hangingCount(1.5F));
        assertEquals(6, BatColony.hangingCount(3.0F));

        CreatureTestScene test = new CreatureTestScene(7, CreatureTestScene.MOCKUP_BUTTONS);
        BatColony colony = new BatColony(test.scene);
        test.frame(colony, CreatureTestScene.FRAME);
        assertEquals(2, colony.hangingBats());

        // More bats fly in after a while, surplus ones drop off right away
        test.setAmount(3.0F);
        int most = 0;
        for (int frame = 0; frame < 20 * 60; frame++) {
            test.frame(colony, CreatureTestScene.FRAME);
            most = Math.max(most, colony.hangingBats());
        }
        assertTrue(most >= 5, "only " + most + " bats hung at 300%");
        test.setAmount(0.25F);
        test.frame(colony, CreatureTestScene.FRAME);
        assertTrue(colony.hangingBats() <= 1);
        for (int k = 1; k < colony.roostCount(); k++) assertTrue(colony.roost(k).state != BatColony.HANGING_STATE);
    }

    @Test
    void aSleepingBatOpensItsEyesThenDropsOffAndAnotherOneComesBack() {
        CreatureTestScene test = new CreatureTestScene(13, CreatureTestScene.MOCKUP_BUTTONS);
        BatColony colony = new BatColony(test.scene);
        test.frame(colony, CreatureTestScene.FRAME);
        BatColony.Roost roost = colony.roost(0);
        assertEquals(BatColony.HANGING_STATE, roost.state);
        float batX = colony.roostX(0) + 2.0F;
        float batY = roost.surface.bottom() + 3.0F;
        roost.timer = 0.0F;

        test.movePointer(batX + 60.0F, batY);
        test.frame(colony, CreatureTestScene.FRAME);
        assertEquals(BatColony.HANGING_STATE, roost.state);
        assertTrue(roost.awake, "the bat should open its eyes");

        int freeBefore = colony.freeBatCount();
        test.movePointer(batX + 20.0F, batY);
        test.frame(colony, CreatureTestScene.FRAME);
        assertEquals(BatColony.EMPTY, roost.state);
        assertEquals(freeBefore + 1, colony.freeBatCount());

        test.hidePointer();
        boolean back = false;
        for (int frame = 0; (frame < 30 * 60) && !back; frame++) {
            test.frame(colony, CreatureTestScene.FRAME);
            back = roost.state == BatColony.HANGING_STATE;
        }
        assertTrue(back, "no bat came back");
        assertFalse(roost.awake);
    }

    @Test
    void theAmountSetsHowOftenGroupsCrossAndBiggerBatsFlyFurtherApart() {
        float normal = averageGroups(1.0F);
        float many = averageGroups(3.0F);
        assertTrue((many / normal > 2.2F) && (many / normal < 3.8F), "groups at 100%: " + normal + ", at 300%: " + many);

        assertEquals(1.0F, BatColony.gap(1), 1.0E-6F);
        assertEquals(2.4F, BatColony.gap(3), 1.0E-6F);
    }

    private static float averageGroups(float amount) {
        CreatureTestScene test = new CreatureTestScene(19, List.of());
        test.setAmount(amount);
        BatColony colony = new BatColony(test.scene);
        long sum = 0;
        int frames = 600 * 60;
        for (int frame = 0; frame < frames; frame++) {
            test.frame(colony, CreatureTestScene.FRAME);
            sum += colony.groupCount();
        }
        return sum / (float)frames;
    }

    @Test
    void batsDodgeThePointerAndFindBackIntoTheirGroup() {
        CreatureTestScene test = new CreatureTestScene(31, List.of());
        BatColony colony = new BatColony(test.scene);
        BatColony.Group group = null;
        for (int frame = 0; (frame < 60 * 60) && (group == null); frame++) {
            test.frame(colony, CreatureTestScene.FRAME);
            for (int i = 0; i < colony.groupCount(); i++) {
                BatColony.Group g = colony.group(i);
                if (!g.far && (g.x > 150.0F) && (g.x < 330.0F)) group = g;
            }
        }
        assertTrue(group != null, "no near group crossed");
        BatColony.Bat bat = group.bats[0];
        // The pointer comes from below, so the bat dodges upwards
        for (int frame = 0; frame < 15; frame++) {
            float x = group.x - group.dir * bat.offsetX;
            float y = group.y + bat.offsetY + (float)Math.sin(test.scene.time() * 2.0F + bat.phase) * 14.0F + bat.dodgeY;
            test.movePointer(x, y + 6.0F);
            test.frame(colony, CreatureTestScene.FRAME);
        }
        assertTrue(bat.dodgeY < -2.0F, "the bat did not dodge: " + bat.dodgeY);
        test.hidePointer();
        test.run(colony, 3.0F);
        assertEquals(0.0F, bat.dodgeY, 0.5F);
    }

    @Test
    void hangingBatsDropOffWhenTheirElementGoesAway() {
        CreatureTestScene test = new CreatureTestScene(23, CreatureTestScene.MOCKUP_BUTTONS);
        BatColony colony = new BatColony(test.scene);
        test.frame(colony, CreatureTestScene.FRAME);
        assertEquals(2, colony.hangingBats());
        int freeBefore = colony.freeBatCount();
        test.setBoxes(List.of());
        test.frame(colony, CreatureTestScene.FRAME);
        assertEquals(0, colony.roostCount());
        assertEquals(0, colony.hangingBats());
        assertEquals(freeBefore + 2, colony.freeBatCount());
    }

}
