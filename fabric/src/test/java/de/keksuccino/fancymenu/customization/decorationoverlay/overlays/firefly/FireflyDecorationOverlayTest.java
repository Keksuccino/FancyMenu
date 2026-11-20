package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireflyDecorationOverlayTest {

    @Test
    void legacyKeysKeepTheirDefaultsAndNewOptionsStartSimpleWithoutBodies() {
        FireflyDecorationOverlay overlay = new FireflyDecorationOverlay();

        assertAll(() -> assertEquals(1.0F, overlay.getProperty("firefly_group_density").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("firefly_group_amount").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("firefly_group_size").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("firefly_scale").getDefault()), () -> assertEquals(true, overlay.getProperty("firefly_follow_mouse").getDefault()), () -> assertEquals(true, overlay.getProperty("firefly_landing").getDefault()), () -> assertEquals("#FFE08A", overlay.getProperty("firefly_color_hex").getDefault()), () -> assertEquals(DecorationOverlayLook.SIMPLE, overlay.look.get()), () -> assertFalse(overlay.fireflyUnlitBodies.getBoolean()), () -> assertNotNull(overlay.getProperty("look")), () -> assertNotNull(overlay.getProperty("firefly_unlit_bodies")));
    }

    @Test
    void olderLayoutsLoadInTheSimpleLookWithTheirSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "fireflies");
        serialized.putProperty("show_decoration_overlay", "true");
        serialized.putProperty("firefly_color_hex", "#80C0FF");
        serialized.putProperty("firefly_scale", "2.0");

        FireflyDecorationOverlay overlay = deserialize(serialized);
        overlay.refreshSettings();

        assertAll(() -> assertEquals(DecorationOverlayLook.SIMPLE, overlay.look.get()), () -> assertTrue(overlay.isSimple()), () -> assertEquals(0x80C0FF, overlay.getArt().color & 0xFFFFFF), () -> assertEquals(0xFF80C0FF, overlay.getArt().simple[2].color('c')), () -> assertEquals(2, FireflyDecorationOverlay.snapScale(overlay.fireflyScale.getFloat())));
    }

    @Test
    void theDefaultColorUsesTheHandPickedColors() {
        FireflyArt art = FireflyArt.of(0xFFFFE08A);
        assertAll(() -> assertSame(FireflyArt.DEFAULT, art), () -> assertSame(FireflyArt.DEFAULT, FireflyArt.of(0x00FFE08A)), () -> assertEquals(0xFFFFFDE4, art.detailed[3].color('c')), () -> assertEquals(0xFFFFF27E, art.detailed[3].color('y')), () -> assertEquals(0x3CA8D24E, art.detailed[3].color('o')), () -> assertEquals(0xD7D9C46A, art.simple[1].color('c')), () -> assertEquals(0x3CFFE08A, art.simple[2].color('a')), () -> assertEquals(0x2AFFE08A, art.simple[3].color('b')), () -> assertEquals(-1, art.tint));
    }

    @Test
    void otherColorsGetADerivedRamp() {
        FireflyArt art = FireflyArt.of(0xFF40A0FF);
        FireflyArt translucent = FireflyArt.of(0x8040A0FF);
        assertAll(() -> assertEquals(0xFF40A0FF, art.simple[2].color('c')), () -> assertEquals(60, art.simple[2].color('a') >>> 24), () -> assertTrue(brightness(art.detailed[3].color('c')) > brightness(art.detailed[2].color('c'))), () -> assertTrue(brightness(art.detailed[2].color('c')) > brightness(art.detailed[1].color('c'))), () -> assertTrue(brightness(art.simple[3].color('c')) > brightness(art.simple[1].color('c'))), () -> assertEquals(0x80FFFFFF, translucent.tint), () -> assertEquals(-1, art.tint));
    }

    @Test
    void aFlashRisesPeaksAndFadesThroughThreeSteps() {
        Firefly single = new Firefly();
        Firefly twice = new Firefly();
        twice.doubleFlash = true;
        float[] times = {-1.0F, 0.03F, 0.2F, 0.5F, 0.9F, 1.2F};
        int[] singleLevels = {0, 2, 3, 2, 1, 0};
        for (int i = 0; i < times.length; i++) {
            single.flashTime = times[i];
            assertEquals(singleLevels[i], single.level(), "single flash at " + times[i]);
        }
        float[] doubleTimes = {0.03F, 0.1F, 0.3F, 0.53F, 0.7F, 1.0F, 1.4F};
        int[] doubleLevels = {2, 3, 1, 2, 3, 2, 1};
        for (int i = 0; i < doubleTimes.length; i++) {
            twice.flashTime = doubleTimes[i];
            assertEquals(doubleLevels[i], twice.level(), "double flash at " + doubleTimes[i]);
        }
    }

    @Test
    void flashesEndAndComeBackAfterAFewDarkSeconds() {
        PixelRandom random = new PixelRandom(5);
        Firefly firefly = new Firefly();
        firefly.nextFlash = 0.05F;
        assertFalse(firefly.advanceFlash(0.04F, random, true));
        assertTrue(firefly.advanceFlash(0.04F, random, true), "the flash starts once its time is up");
        int steps = 0;
        while ((firefly.flashTime >= 0.0F) && (steps++ < 100)) firefly.advanceFlash(0.05F, random, true);
        assertAll(() -> assertEquals(-1.0F, firefly.flashTime), () -> assertTrue((firefly.nextFlash >= 2.2F) && (firefly.nextFlash < 5.5F)));
    }

    @Test
    void aFlashNearbyPullsDarkFirefliesForward() {
        Firefly waiting = new Firefly();
        waiting.nextFlash = 2.0F;
        Firefly almost = new Firefly();
        almost.nextFlash = 0.3F;
        Firefly glowing = new Firefly();
        glowing.flashTime = 0.2F;
        glowing.nextFlash = 3.0F;

        waiting.nudge();
        almost.nudge();
        glowing.nudge();

        assertAll(() -> assertEquals(1.6F, waiting.nextFlash, 0.0001F), () -> assertEquals(0.3F, almost.nextFlash), () -> assertEquals(3.0F, glowing.nextFlash));
    }

    @Test
    void firefliesRestingOnElementsAreElementContentAndOnTheFloorScreenContent() {
        FireflyDecorationOverlay overlay = new FireflyDecorationOverlay();
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(new CollisionAreaBounds(100, 80, 60, 20)), 480, 270);
        Firefly onElement = new Firefly();
        Firefly onFloor = new Firefly();

        for (int i = 0; i < 20; i++) {
            overlay.aimAt(onElement, surfaces.elements().get(0), 270);
            float x = onElement.targetX;
            float y = onElement.targetY;
            // On one of the four edges of the element
            assertTrue((x >= 100.0F) && (x < 160.0F) && (y >= 79.0F) && (y <= 100.0F), "spot " + x + "," + y);
            assertTrue(onElement.targetOnElement);
        }
        overlay.aimAt(onFloor, surfaces.floor(), 270);
        assertAll(() -> assertFalse(onFloor.targetOnElement), () -> assertEquals(269.0F, onFloor.targetY));

        onElement.state = Firefly.LANDING;
        assertFalse(FireflyDecorationOverlay.isRestingOnElement(onElement), "still flying to the element");
        onElement.state = Firefly.LANDED;
        onFloor.state = Firefly.LANDED;
        assertAll(() -> assertTrue(FireflyDecorationOverlay.isRestingOnElement(onElement)), () -> assertFalse(FireflyDecorationOverlay.isRestingOnElement(onFloor)));
    }

    @Test
    void scaleSnapsToWholeNumbers() {
        assertAll(() -> assertEquals(1, FireflyDecorationOverlay.snapScale(1.0F)), () -> assertEquals(1, FireflyDecorationOverlay.snapScale(1.4F)), () -> assertEquals(2, FireflyDecorationOverlay.snapScale(1.5F)), () -> assertEquals(4, FireflyDecorationOverlay.snapScale(9.0F)), () -> assertEquals(1, FireflyDecorationOverlay.snapScale(0.2F)), () -> assertEquals(1, FireflyDecorationOverlay.snapScale(Float.NaN)));
    }

    @Test
    void fireflyArtSpritesCenterTheirCore() {
        for (int level = 1; level <= 3; level++) {
            int size = FireflyArt.HALOS[level].width();
            assertEquals('c', FireflyArt.HALOS[level].codeAt(size / 2, size / 2), "detailed level " + level);
            for (int big = 0; big <= 1; big++) {
                int width = FireflyArt.SIMPLE[big][level].width();
                int offset = (width - ((big == 1) ? 2 : 1)) / 2;
                assertEquals('c', FireflyArt.SIMPLE[big][level].codeAt(offset, offset), "simple level " + level + " big " + big);
            }
        }
    }

    private static int brightness(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    private static FireflyDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<FireflyDecorationOverlay> instances = new FireflyDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
