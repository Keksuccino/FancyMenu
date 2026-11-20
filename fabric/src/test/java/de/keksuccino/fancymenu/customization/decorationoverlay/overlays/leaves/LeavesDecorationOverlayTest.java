package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeavesDecorationOverlayTest {

    /** The mockup's ramps of the Custom Range with the default colors #7BA84F and #D58A3B */
    private static final int[][] MOCKUP_DEFAULT_RANGE = {
            {0xADC171, 0x7BA84F, 0x626E43, 0x6D8848, 0x444724},
            {0xBBBC6E, 0x92A14A, 0x716A40, 0x808345, 0x4B4523},
            {0xC9B76B, 0xA89945, 0x7E653D, 0x917C41, 0x524321},
            {0xD7B368, 0xBF9240, 0x8C613A, 0xA3773D, 0x5A401F},
            {0xE5AE64, 0xD58A3B, 0x9A5C37, 0xB57139, 0x613E1E}
    };
    /** The mockup's ramps of a range from #FFD6E4 to #F48FB1 */
    private static final int[][] MOCKUP_PINK_RANGE = {
            {0xFFDDCD, 0xFFD6E4, 0xB48BA0, 0xD6ADBF, 0x6E5654},
            {0xFDD2C5, 0xFCC4D7, 0xB28098, 0xD39FB4, 0x6D5050},
            {0xFCC8BE, 0xFAB3CB, 0xB17590, 0xD291AB, 0x6D4B4C},
            {0xFABCB6, 0xF7A1BE, 0xAF6A88, 0xCF83A0, 0x6C4548},
            {0xF8B1AE, 0xF48FB1, 0xAD5F80, 0xCD7596, 0x6B3F44}
    };

    @Test
    void legacyKeysKeepTheirDefaultsAndNewLeavesStartInAutumnMix() {
        LeavesDecorationOverlay overlay = new LeavesDecorationOverlay();

        assertAll(() -> assertEquals(1.0F, overlay.getProperty("leaves_density").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("leaves_wind_intensity").getDefault()), () -> assertEquals(true, overlay.getProperty("leaves_wind_blows").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("leaves_fall_speed").getDefault()), () -> assertEquals(1.0F, overlay.getProperty("leaves_scale").getDefault()), () -> assertEquals("#7BA84F", overlay.getProperty("leaves_color_start_hex").getDefault()), () -> assertEquals("#D58A3B", overlay.getProperty("leaves_color_end_hex").getDefault()), () -> assertEquals(LeavesColorMode.AUTUMN_MIX, overlay.leavesColors.get()), () -> assertEquals(DecorationOverlayLook.DETAILED, overlay.look.get()), () -> assertNotNull(overlay.getProperty("leaves_colors")));
    }

    @Test
    void leavesSwitchedOnInAnOlderLayoutKeepTheirColorsThroughTheCustomRange() {
        PropertyContainer serialized = legacyContainer();
        serialized.putProperty("show_decoration_overlay", "true");
        serialized.putProperty("leaves_color_start_hex", "#FFD6E4");
        serialized.putProperty("leaves_color_end_hex", "#F48FB1");

        LeavesDecorationOverlay overlay = deserialize(serialized);
        overlay.refreshSettings();

        assertEquals(LeavesColorMode.CUSTOM_RANGE, overlay.leavesColors.get());
        assertRamps(MOCKUP_PINK_RANGE, overlay.getArt());
    }

    @Test
    void leavesNeverSwitchedOnInAnOlderLayoutStartInAutumnMix() {
        PropertyContainer hidden = legacyContainer();
        hidden.putProperty("show_decoration_overlay", "false");
        hidden.putProperty("leaves_color_start_hex", "#FFD6E4");

        LeavesDecorationOverlay overlay = deserialize(hidden);
        overlay.refreshSettings();

        assertAll(() -> assertEquals(LeavesColorMode.AUTUMN_MIX, overlay.leavesColors.get()), () -> assertSame(LeafArt.AUTUMN, overlay.getArt()));
    }

    @Test
    void savedColorModeIsKept() {
        PropertyContainer serialized = legacyContainer();
        serialized.putProperty("show_decoration_overlay", "true");
        serialized.putProperty("leaves_colors", "autumn_mix");

        LeavesDecorationOverlay overlay = deserialize(serialized);

        assertEquals(LeavesColorMode.AUTUMN_MIX, overlay.leavesColors.get());
    }

    @Test
    void switchedOnNewLeavesStayInAutumnMixAfterSaving() {
        LeavesDecorationOverlay original = new LeavesDecorationOverlay();
        original.showOverlay.set(true);

        LeavesDecorationOverlay loaded = deserialize(new LeavesDecorationOverlayBuilder()._serialize(original));

        assertEquals(LeavesColorMode.AUTUMN_MIX, loaded.leavesColors.get());
    }

    @Test
    void customRangeDerivesFiveRampsLikeTheMockup() {
        assertRamps(MOCKUP_DEFAULT_RANGE, LeafArt.ofRange(0x7BA84F, 0xD58A3B));
        int[][] ramps = LeafArt.customRamps(0x7BA84F, 0xD58A3B);
        assertAll(() -> assertEquals(0x7BA84F, ramps[0][1]), () -> assertEquals(0xD58A3B, ramps[4][1]));
    }

    @Test
    void changingTheColorsRebuildsTheCustomRange() {
        LeavesDecorationOverlay overlay = new LeavesDecorationOverlay();
        overlay.leavesColors.set(LeavesColorMode.CUSTOM_RANGE);
        overlay.refreshSettings();
        assertRamps(MOCKUP_DEFAULT_RANGE, overlay.getArt());

        overlay.leavesColorStartHex.set("#FFD6E4");
        overlay.leavesColorEndHex.set("#F48FB1");
        overlay.refreshSettings();
        assertRamps(MOCKUP_PINK_RANGE, overlay.getArt());

        overlay.leavesColors.set(LeavesColorMode.AUTUMN_MIX);
        overlay.refreshSettings();
        assertSame(LeafArt.AUTUMN, overlay.getArt());
    }

    @Test
    void autumnMixUsesTheFiveBuiltInRampsAndADarkerUnderside() {
        LeafArt art = LeafArt.AUTUMN;
        assertRamps(LeafArt.AUTUMN_RAMPS, art);
        for (int i = 0; i < LeafArt.RAMP_COUNT; i++) {
            int[] ramp = LeafArt.AUTUMN_RAMPS[i];
            PixelPalette back = art.back[i];
            PixelPalette simple = art.simple[i];
            assertAll("ramp " + i, () -> assertEquals(0xFF000000 | ramp[1], back.color('h')), () -> assertEquals(0xFF000000 | ramp[2], back.color('b')), () -> assertEquals(0xFF000000 | ramp[2], back.color('v')), () -> assertEquals(0xFF000000 | ramp[1], simple.color('b')), () -> assertEquals(0xFF000000 | ramp[2], simple.color('d')), () -> assertEquals(0xFF000000 | ramp[4], simple.color('s')), () -> assertEquals(0, simple.color('h')));
        }
    }

    @Test
    void landedLeavesDissolveInADitherPattern() {
        PixelSprite half = LeafArt.FLAT_DISSOLVING;
        PixelSprite thin = LeafArt.FLAT_DISSOLVED;
        for (int y = 0; y < LeafArt.FLAT.height(); y++) {
            for (int x = 0; x < LeafArt.FLAT.width(); x++) {
                char original = LeafArt.FLAT.codeAt(x, y);
                boolean odd = ((x + y) & 1) != 0;
                assertEquals(odd ? original : '.', half.codeAt(x, y), "half at " + x + "," + y);
                assertEquals((odd && ((x & 1) == 0)) ? original : '.', thin.codeAt(x, y), "thin at " + x + "," + y);
            }
        }
    }

    @Test
    void mapleLeavesSpinInQuarterTurns() {
        PixelSprite[] turns = LeafArt.MAPLE_TURNS;
        PixelSprite full = turns[3].rotatedClockwise();
        for (int y = 0; y < full.height(); y++) {
            for (int x = 0; x < full.width(); x++) {
                assertEquals(turns[0].codeAt(x, y), full.codeAt(x, y));
            }
        }
        assertEquals(11, LeafArt.BIG_MAPLE_TURNS[1].width());
    }

    @Test
    void leavesLyingOnElementsAreElementContentAndOnTheFloorScreenContent() {
        LeavesDecorationOverlay overlay = new LeavesDecorationOverlay();
        PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
        surfaces.update(List.of(new CollisionAreaBounds(100, 80, 60, 20)), 480, 270);
        Leaf onElement = new Leaf();
        Leaf onFloor = new Leaf();

        overlay.land(onElement, surfaces.elements().get(0), 157.0F);
        overlay.land(onFloor, surfaces.floor(), 30.0F);

        assertAll(() -> assertTrue(onElement.landed), () -> assertTrue(onElement.landedOnElement), () -> assertEquals(153, onElement.landX, "kept fully on the element"), () -> assertEquals(77, onElement.landY, "lies on top of the element"), () -> assertTrue(onFloor.landed), () -> assertFalse(onFloor.landedOnElement), () -> assertEquals(30, onFloor.landX), () -> assertEquals(267, onFloor.landY));
    }

    @Test
    void scaleSnapsToWholeNumbers() {
        assertAll(() -> assertEquals(1, LeavesDecorationOverlay.snapScale(1.2F)), () -> assertEquals(3, LeavesDecorationOverlay.snapScale(2.5F)), () -> assertEquals(5, LeavesDecorationOverlay.snapScale(8.0F)), () -> assertEquals(1, LeavesDecorationOverlay.snapScale(-1.0F)));
    }

    @Test
    void respawnedLeavesStartAboveTheScreenAndFarLeavesAreNeverBig() {
        PixelRandom random = new PixelRandom(11);
        Leaf leaf = new Leaf();
        for (int i = 0; i < 200; i++) {
            leaf.respawn(false, true, 480, 270, random);
            assertAll(() -> assertTrue(leaf.y < 0.0F), () -> assertFalse(leaf.big), () -> assertTrue((leaf.ramp >= 0) && (leaf.ramp < LeafArt.RAMP_COUNT)), () -> assertTrue((leaf.amplitude >= 4.0F) && (leaf.amplitude < 9.0F)));
        }
    }

    @Test
    void gustsCrossTheScreenAndEndPastIt() {
        Gust gust = new Gust();
        PixelRandom random = new PixelRandom(3);
        gust.start(-1, 520.0F, 270, random);
        int firstId = gust.id();
        int frames = 0;
        while (gust.isActive() && (frames++ < 1000)) gust.advance(0.05F, 480, 130.0F);
        assertAll(() -> assertFalse(gust.isActive()), () -> assertTrue(gust.x() < -130.0F), () -> assertEquals(-1, gust.direction()));
        gust.start(1, -40.0F, 270, random);
        assertAll(() -> assertTrue(gust.isActive()), () -> assertEquals(firstId + 1, gust.id()), () -> assertEquals(0.0F, gust.time()));
    }

    private static void assertRamps(int[][] expected, LeafArt art) {
        for (int i = 0; i < expected.length; i++) {
            PixelPalette face = art.face[i];
            char[] codes = {'h', 'b', 'd', 'v', 's'};
            for (int k = 0; k < codes.length; k++) {
                int actual = face.color(codes[k]);
                int wanted = expected[i][k];
                assertEquals(255, actual >>> 24);
                // Colors mixed in floats can round one step away from the mockup's doubles
                for (int shift = 0; shift <= 16; shift += 8) {
                    int a = (actual >> shift) & 0xFF;
                    int w = (wanted >> shift) & 0xFF;
                    assertTrue(Math.abs(a - w) <= 1, "ramp " + i + " tone " + codes[k] + ": expected " + Integer.toHexString(wanted) + " but was " + Integer.toHexString(actual & 0xFFFFFF));
                }
            }
        }
    }

    private static PropertyContainer legacyContainer() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "leaves");
        return serialized;
    }

    private static LeavesDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<LeavesDecorationOverlay> instances = new LeavesDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
