package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.skylanterns;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureTestScene;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanternSkyTest {

    @Test
    void theAmountAndTheScreenSetHowManyLanternsFloatUp() {
        CreatureTestScene test = new CreatureTestScene(3, List.of());
        LanternSky sky = new LanternSky(test.scene);
        test.frame(sky, CreatureTestScene.FRAME);
        assertLayers(sky, 22, 10, 5);

        test.setAmount(3.0F);
        test.frame(sky, CreatureTestScene.FRAME);
        assertLayers(sky, 66, 30, 15);

        test.setAmount(0.25F);
        test.frame(sky, CreatureTestScene.FRAME);
        assertLayers(sky, 6, 3, 1);

        test.setAmount(1.0F);
        test.setSize(960, 540);
        test.frame(sky, CreatureTestScene.FRAME);
        assertLayers(sky, 88, 40, 20);
    }

    private static void assertLayers(LanternSky sky, int far, int middle, int near) {
        assertEquals(far, sky.layerCount(0));
        assertEquals(middle, sky.layerCount(1));
        assertEquals(near, sky.layerCount(2));
    }

    @Test
    void groupsGetReleasedTogetherFromOneSpotAndAreGoneOnceTheyLeave() {
        CreatureTestScene test = new CreatureTestScene(5, List.of());
        LanternSky sky = new LanternSky(test.scene);
        test.run(sky, 5.9F);
        assertEquals(0, sky.releasedCount());
        test.run(sky, 0.2F);
        int released = sky.releasedCount();
        assertTrue((released >= 6) && (released <= 10), "released " + released);
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < released; i++) {
            LanternSky.Lantern l = sky.releasedLantern(i);
            assertTrue((l.depth == 1) || (l.depth == LanternSky.NEAR), "released lanterns are middle or near ones");
            assertTrue(l.y > 270.0F);
            min = Math.min(min, l.x);
            max = Math.max(max, l.x);
        }
        assertTrue(max - min <= 120.0F, "the group spreads over " + (max - min) + " pixels");

        // At 300% a group is three times as big
        CreatureTestScene many = new CreatureTestScene(5, List.of());
        many.setAmount(3.0F);
        LanternSky busySky = new LanternSky(many.scene);
        many.run(busySky, 6.1F);
        assertTrue(busySky.releasedCount() >= 18, "released " + busySky.releasedCount());

        // Released lanterns are gone as soon as they rise out of the screen
        for (int frame = 0; frame < 120 * 60; frame++) {
            test.frame(sky, CreatureTestScene.FRAME);
            for (int i = 0; i < sky.releasedCount(); i++) assertTrue(sky.releasedLantern(i).y >= -12.0F);
            assertTrue(sky.releasedCount() <= 60);
        }
    }

    @Test
    void theHaloIsTheMockupsDitheredDiamond() {
        // lightHalo(5) of the mockup
        String[] expected = {
                "...........",
                ".....a.....",
                "....a.a....",
                "...a.a.a...",
                "..a.a.a.a..",
                ".a.a.a.a.a.",
                "..a.a.a.a..",
                "...a.a.a...",
                "....a.a....",
                ".....a.....",
                "..........."};
        PixelSprite halo = LanternSky.halo(5);
        assertEquals(11, halo.width());
        assertEquals(11, halo.height());
        for (int y = 0; y < 11; y++) {
            for (int x = 0; x < 11; x++) assertEquals(expected[y].charAt(x), halo.codeAt(x, y), "pixel " + x + ", " + y);
        }
    }

}
