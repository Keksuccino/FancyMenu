package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.shootingstars;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorShowerTest {

    private static final float STEP = 1.0F / 60.0F;
    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;

    /**
     * Runs the shower for some seconds at 60 updates per second and records every launch.
     * A meteor that just launched has moved exactly one update, pooled meteors start over at a life of 0.
     */
    private static List<Launch> run(MeteorShower shower, PixelRandom random, float startTime, float seconds) {
        List<Launch> launches = new ArrayList<>();
        int steps = Math.round(seconds / STEP);
        for (int i = 1; i <= steps; i++) {
            // The last meteor of a shower ends it, so the update before counts too
            boolean showerBefore = shower.isShowerActive();
            shower.update(STEP, random, WIDTH, HEIGHT);
            for (int m = 0; m < shower.count; m++) {
                MeteorShower.Meteor meteor = shower.meteors[m];
                if (meteor.life != STEP) continue;
                float startY = meteor.y - meteor.dirY * meteor.speed * STEP;
                launches.add(new Launch(startTime + i * STEP, meteor.dirX, startY, meteor.maxLife, meteor.length, meteor.speed, showerBefore || shower.isShowerActive()));
            }
        }
        return launches;
    }

    @Test
    void theFirstMeteorComesAfterOnePointTwoSeconds() {
        List<Launch> launches = run(new MeteorShower(), new PixelRandom(5), 0.0F, 3.0F);
        assertFalse(launches.isEmpty());
        assertEquals(MeteorShower.FIRST_SINGLE_DELAY, launches.getFirst().time, 2 * STEP);
    }

    @Test
    void singleMeteorsComeEveryFewSeconds() {
        for (int seed = 1; seed <= 10; seed++) {
            // No shower before nine seconds, so all of these are single meteors
            List<Launch> launches = run(new MeteorShower(), new PixelRandom(seed), 0.0F, MeteorShower.FIRST_SHOWER_DELAY - STEP);
            for (int i = 1; i < launches.size(); i++) {
                float gap = launches.get(i).time - launches.get(i - 1).time;
                assertTrue((gap >= MeteorShower.SINGLE_MIN_INTERVAL - STEP) && (gap <= MeteorShower.SINGLE_MAX_INTERVAL + STEP), "seed " + seed + " gap " + gap);
            }
        }
    }

    @Test
    void aShowerStartsAfterNineSecondsAndSendsItsMeteorsFromOneDirection() {
        for (int seed = 1; seed <= 25; seed++) {
            MeteorShower shower = new MeteorShower();
            PixelRandom random = new PixelRandom(seed);
            float before = MeteorShower.FIRST_SHOWER_DELAY - 0.5F;
            run(shower, random, 0.0F, before);
            assertFalse(shower.isShowerActive());
            List<Launch> launches = run(shower, random, before, 6.0F);
            assertFalse(shower.isShowerActive(), "seed " + seed + ": a shower lasts at most 11 x 0.4 seconds");
            int direction = shower.getShowerDirection();
            int sameWay = 0;
            float first = Float.MAX_VALUE;
            float last = 0.0F;
            for (Launch launch : launches) {
                if (!launch.duringShower || (Math.signum(launch.dirX) != direction)) continue;
                sameWay++;
                first = Math.min(first, launch.time);
                last = Math.max(last, launch.time);
            }
            // A single meteor can fly the shower's way during it, at most three fit into the longest shower
            assertTrue((sameWay >= MeteorShower.SHOWER_MIN_METEORS) && (sameWay <= MeteorShower.SHOWER_MAX_METEORS + 3), "seed " + seed + " sent " + sameWay);
            assertTrue(first >= MeteorShower.FIRST_SHOWER_DELAY - STEP, "seed " + seed + " started at " + first);
            assertTrue(last - first <= MeteorShower.SHOWER_MAX_METEORS * MeteorShower.SHOWER_MAX_SPACING + STEP, "seed " + seed + " lasted " + (last - first));
        }
    }

    @Test
    void meteorsFallDownAtEighteenToThirtyEightDegrees() {
        List<Launch> launches = run(new MeteorShower(), new PixelRandom(3), 0.0F, 40.0F);
        assertTrue(launches.size() > 15);
        for (Launch launch : launches) {
            double degrees = Math.toDegrees(Math.acos(Math.abs(launch.dirX)));
            assertTrue((degrees >= 17.9) && (degrees <= 38.1), "angle " + degrees);
            assertTrue((launch.startY >= -10.5F) && (launch.startY <= 90.5F), "start " + launch.startY);
            assertTrue((launch.maxLife >= 0.5F) && (launch.maxLife <= 0.9F));
            assertTrue((launch.length >= 18) && (launch.length <= 28));
            assertTrue((launch.speed >= 240.0F) && (launch.speed <= 340.0F));
        }
    }

    @Test
    void meteorsStartHigherOnTallerScreensOnlyInProportion() {
        MeteorShower shower = new MeteorShower();
        PixelRandom random = new PixelRandom(21);
        for (int i = 0; i < 3000; i++) {
            shower.update(STEP, random, 960, 540);
            for (int m = 0; m < shower.count; m++) {
                MeteorShower.Meteor meteor = shower.meteors[m];
                if (meteor.life == STEP) assertTrue(meteor.y - meteor.dirY * meteor.speed * STEP <= 180.5F);
            }
        }
    }

    @Test
    void meteorsBurnOutAfterTheirLife() {
        MeteorShower shower = new MeteorShower();
        PixelRandom random = new PixelRandom(9);
        for (int i = 0; i < 6000; i++) {
            shower.update(STEP, random, WIDTH, HEIGHT);
            assertTrue(shower.count <= MeteorShower.CAPACITY);
            for (int m = 0; m < shower.count; m++) assertTrue(shower.meteors[m].life <= shower.meteors[m].maxLife);
        }
    }

    @Test
    void theTrailGrowsInAndShrinksAway() {
        MeteorShower.Meteor meteor = new MeteorShower.Meteor();
        meteor.length = 20;
        meteor.maxLife = 1.0F;
        meteor.life = 0.0F;
        assertEquals(0, meteor.currentLength());
        meteor.life = 0.1F;
        assertEquals(10, meteor.currentLength());
        meteor.life = 0.5F;
        assertEquals(20, meteor.currentLength());
        meteor.life = 0.875F;
        assertEquals(10, meteor.currentLength());
        assertTrue(meteor.isBreakingUp());
        meteor.life = 0.8F;
        assertFalse(meteor.isBreakingUp());
    }

    private record Launch(float time, float dirX, float startY, float maxLife, int length, float speed, boolean duringShower) {
    }

}
