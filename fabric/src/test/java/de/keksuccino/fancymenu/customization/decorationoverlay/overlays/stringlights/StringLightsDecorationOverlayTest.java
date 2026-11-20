package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class StringLightsDecorationOverlayTest {

    private static final List<String> LEGACY_POSITION_NAMES = List.of("left_center_to_top_center", "right_center_to_top_center", "bottom_left_to_top_center", "bottom_right_to_top_center", "top_left_to_top_right", "bottom_left_to_bottom_right", "loose_left_top", "loose_right_top");

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disablePlaceholderCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restorePlaceholderCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void legacyPropertyKeysAndDefaultsStayUnchanged() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();

        assertAll(() -> assertNotNull(overlay.getProperty("string_lights_scale")), () -> assertNotNull(overlay.getProperty("string_lights_wind_strength")), () -> assertNotNull(overlay.getProperty("string_lights_flicker_speed")));
        for (String name : LEGACY_POSITION_NAMES) {
            String keyBase = "string_lights_" + name;
            assertAll(name, () -> assertNotNull(overlay.getProperty(keyBase)), () -> assertNotNull(overlay.getProperty(keyBase + "_christmas_mode")), () -> assertNotNull(overlay.getProperty(keyBase + "_color_hex")), () -> assertNotNull(overlay.getProperty(keyBase + "_light_count")), () -> assertNotNull(overlay.getProperty(keyBase + "_string_color_hex")), () -> assertNotNull(overlay.getProperty(keyBase + "_light_sizes")), () -> assertNotNull(overlay.getProperty(keyBase + "_light_colors")), () -> assertEquals("#FFD27A", overlay.getProperty(keyBase + "_color_hex").getDefault()), () -> assertEquals(false, overlay.getProperty(keyBase + "_christmas_mode").getDefault()));
        }
        assertAll(() -> assertEquals(true, overlay.getProperty("string_lights_left_center_to_top_center").getDefault()), () -> assertEquals(true, overlay.getProperty("string_lights_right_center_to_top_center").getDefault()), () -> assertEquals(true, overlay.getProperty("string_lights_top_left_to_top_right").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_left_to_top_center").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_right_to_top_center").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_left_to_bottom_right").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_loose_left_top").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_loose_right_top").getDefault()));
    }

    @Test
    void newOverlaysStartDetailedWithoutGarlandAndCycleThePrograms() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();

        assertAll(() -> assertEquals(DecorationOverlayLook.DETAILED, overlay.look.get()), () -> assertFalse(overlay.pineGarland.getBoolean()), () -> assertEquals(StringLightsProgram.CYCLE, overlay.program.get()), () -> assertEquals("look", overlay.look.property.getKey()), () -> assertEquals("string_lights_pine_garland", overlay.pineGarland.getKey()), () -> assertEquals("string_lights_program", overlay.program.getKey()));
    }

    @Test
    void stringLightsSwitchedOnInAnOlderLayoutStartSimpleAndTwinkle() {
        PropertyContainer serialized = legacyContainer();
        serialized.putProperty("show_decoration_overlay", "true");

        StringLightsDecorationOverlay overlay = deserialize(serialized);

        assertAll(() -> assertEquals(DecorationOverlayLook.SIMPLE, overlay.look.get()), () -> assertEquals(StringLightsProgram.TWINKLE, overlay.program.get()));
    }

    @Test
    void stringLightsNeverSwitchedOnInAnOlderLayoutCountAsNew() {
        PropertyContainer hidden = legacyContainer();
        hidden.putProperty("show_decoration_overlay", "false");
        PropertyContainer withoutShowKey = legacyContainer();

        StringLightsDecorationOverlay hiddenOverlay = deserialize(hidden);
        StringLightsDecorationOverlay overlayWithoutShowKey = deserialize(withoutShowKey);

        assertAll(() -> assertEquals(DecorationOverlayLook.DETAILED, hiddenOverlay.look.get()), () -> assertEquals(StringLightsProgram.CYCLE, hiddenOverlay.program.get()), () -> assertEquals(DecorationOverlayLook.DETAILED, overlayWithoutShowKey.look.get()));
    }

    @Test
    void savedLookAndProgramAreKept() {
        PropertyContainer serialized = legacyContainer();
        serialized.putProperty("show_decoration_overlay", "true");
        serialized.putProperty("look", "detailed");
        serialized.putProperty("string_lights_program", "chase");

        StringLightsDecorationOverlay overlay = deserialize(serialized);

        assertAll(() -> assertEquals(DecorationOverlayLook.DETAILED, overlay.look.get()), () -> assertEquals(StringLightsProgram.CHASE, overlay.program.get()));
    }

    @Test
    void switchedOnNewOverlaysStayDetailedAfterSaving() {
        StringLightsDecorationOverlay original = new StringLightsDecorationOverlay();
        original.showOverlay.set(true);
        original.pineGarland.set(true);

        StringLightsDecorationOverlay loaded = deserialize(new StringLightsDecorationOverlayBuilder()._serialize(original));

        assertAll(() -> assertTrue(loaded.showOverlay.tryGetNonNull()), () -> assertEquals(DecorationOverlayLook.DETAILED, loaded.look.get()), () -> assertEquals(StringLightsProgram.CYCLE, loaded.program.get()), () -> assertTrue(loaded.pineGarland.getBoolean()));
    }

    @Test
    void legacyLayoutLoadsItsStrings() {
        PropertyContainer serialized = legacyContainer();
        serialized.putProperty("string_lights_left_center_to_top_center", "false");
        serialized.putProperty("string_lights_loose_left_top", "true");
        serialized.putProperty("string_lights_loose_left_top_christmas_mode", "true");
        serialized.putProperty("string_lights_loose_left_top_color_hex", "#FF0000");

        StringLightsDecorationOverlay overlay = deserialize(serialized);
        overlay.refreshSettings();

        StringLightsDecorationOverlay.StringLightProperties loose = overlay.stringLights.get(StringLightsPosition.LOOSE_LEFT_TOP);
        assertAll(() -> assertFalse(overlay.stringLights.get(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER).enabled), () -> assertTrue(overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT).enabled), () -> assertTrue(loose.enabled), () -> assertTrue(loose.christmas), () -> assertEquals(0xFF0000, loose.ramp.base), () -> assertEquals(0, loose.configuredCount), () -> assertEquals(StringLightsDecorationOverlay.DEFAULT_STRING_COLOR, loose.wireColor), () -> assertEquals(List.of(), loose.sizes), () -> assertEquals(List.of(), loose.customRamps));
    }

    @Test
    void newSettingsSurviveSerializationRoundTrip() {
        StringLightsDecorationOverlay original = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = original.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightCount.set(12);
        topString.stringColor.set("#FF000080");
        topString.lightSizes.set("1.0, 2.0");
        topString.lightColors.set("#00FF00, , #0000FF");
        original.program.set(StringLightsProgram.BREATHE);
        original.look.property.set(DecorationOverlayLook.SIMPLE);

        StringLightsDecorationOverlay loaded = deserialize(new StringLightsDecorationOverlayBuilder()._serialize(original));
        StringLightsDecorationOverlay.StringLightProperties loadedTopString = loaded.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);

        assertAll(() -> assertEquals(12, loadedTopString.lightCount.getInteger()), () -> assertEquals("#FF000080", loadedTopString.stringColor.get()), () -> assertEquals("1.0, 2.0", loadedTopString.lightSizes.get()), () -> assertEquals("#00FF00, , #0000FF", loadedTopString.lightColors.get()), () -> assertEquals(0, loaded.stringLights.get(StringLightsPosition.LOOSE_LEFT_TOP).lightCount.getInteger()), () -> assertEquals(StringLightsProgram.BREATHE, loaded.program.get()), () -> assertEquals(DecorationOverlayLook.SIMPLE, loaded.look.get()));
    }

    @Test
    void settingsOfOneStringAreAppliedOnlyToItsBulbs() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightCount.set(12);
        topString.stringColor.set("#FF000080");
        topString.lightSizes.set("2, 0");
        topString.lightColors.set("#00FF00");

        overlay.refreshSettings();

        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        LightString left = overlay.getString(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER);
        assertAll(() -> assertEquals(12, top.bulbCount), () -> assertEquals(0x80FF0000, topString.wireColor), () -> assertEquals(2, top.bulbScale[0]), () -> assertEquals(0, top.bulbScale[1]), () -> assertEquals(2, top.bulbScale[2]), () -> assertEquals(0x00FF00, top.bulbRamp[5].base), () -> assertEquals(LightString.resolveBulbCount(0, left.length, LightString.BULB_SPACING), left.bulbCount), () -> assertEquals(1, left.bulbScale[0]), () -> assertSame(StringLightsArt.WARM, left.bulbRamp[0]), () -> assertEquals(StringLightsDecorationOverlay.DEFAULT_STRING_COLOR, overlay.stringLights.get(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER).wireColor));
    }

    @Test
    void christmasBulbsRepeatRedGreenBlueAmberPink() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT).christmasMode.set(true);

        overlay.refreshSettings();

        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        int[] order = {0xF0483E, 0x3CC860, 0x3E8CF0, 0xFFC23C, 0xF05CA8};
        assertTrue(top.bulbCount >= 10);
        for (int i = 0; i < top.bulbCount; i++) {
            assertEquals(order[i % order.length], top.bulbRamp[i].base, "bulb " + i);
        }
    }

    @Test
    void baublesAreRedAndGoldOnWarmStringsAndGoldAndSilverAmongColoredBulbs() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT).christmasMode.set(true);
        overlay.stringLights.get(StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER).lightColor.set("#3E8CF0");

        overlay.refreshSettings();

        LightString warm = overlay.getString(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER);
        LightString christmas = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        LightString blue = overlay.getString(StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER);
        assertAll(() -> assertEquals(warm.bulbCount / 2, warm.baubleCount), () -> assertSame(StringLightsArt.BAUBLE_RED, warm.baublePalette[0]), () -> assertSame(StringLightsArt.BAUBLE_GOLD, warm.baublePalette[1]), () -> assertSame(StringLightsArt.BAUBLE_GOLD, christmas.baublePalette[0]), () -> assertSame(StringLightsArt.BAUBLE_SILVER, christmas.baublePalette[1]), () -> assertSame(StringLightsArt.BAUBLE_GOLD, blue.baublePalette[0]), () -> assertSame(StringLightsArt.BAUBLE_SILVER, blue.baublePalette[1]));
        // Baubles hang in every second gap between the bulbs
        for (int j = 0; j < warm.baubleCount; j++) {
            float t = warm.baubleT[j];
            assertTrue((t > warm.bulbT[2 * j]) && (t < warm.bulbT[2 * j + 1]), "bauble " + j);
        }
    }

    @Test
    void outOfRangeSettingsGetClamped() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT).lightCount.set(LightString.MAX_BULBS + 50);
        overlay.stringLights.get(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER).lightCount.set(-4);
        overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT).lightSizes.set("25, -1");
        overlay.stringLightsScale.set(40.0F);

        overlay.refreshSettings();
        overlay.update(0.0F);

        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        assertAll(() -> assertEquals(LightString.MAX_BULBS, top.bulbCount), () -> assertEquals(0, overlay.stringLights.get(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER).configuredCount), () -> assertEquals(4, top.bulbScale[0]), () -> assertEquals(0, top.bulbScale[1]), () -> assertEquals(3, overlay.getSpriteScale()));
    }

    @Test
    void patternsResolvePlaceholdersBeforeSplittingEntries() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightSizes.set(TestPlaceholders.echo("1.5, 2"));
        topString.lightColors.set(TestPlaceholders.echo("#FF0000, #00FF00"));
        topString.lightCount.setManualInput(TestPlaceholders.echo("7"));

        overlay.refreshSettings();

        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        assertAll(() -> assertEquals(List.of(1.5F, 2.0F), topString.sizes), () -> assertEquals(0xFF0000, topString.customRamps.get(0).base), () -> assertEquals(0x00FF00, topString.customRamps.get(1).base), () -> assertEquals(7, top.bulbCount), () -> assertEquals(0xFF0000, top.bulbRamp[2].base));
    }

    @Test
    void changedAndClearedPatternsReachTheBulbs() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);

        topString.lightSizes.set("2");
        topString.lightColors.set("#00FF00");
        overlay.refreshSettings();
        topString.lightSizes.set("3, 4");
        topString.lightColors.set("#0000FF");
        overlay.refreshSettings();

        assertAll(() -> assertEquals(3, top.bulbScale[0]), () -> assertEquals(4, top.bulbScale[1]), () -> assertEquals(0x0000FF, top.bulbRamp[0].base));

        topString.lightSizes.set("");
        topString.lightColors.set("   ");
        overlay.refreshSettings();

        assertAll(() -> assertEquals(1, top.bulbScale[0]), () -> assertEquals(1, top.bulbScale[1]), () -> assertSame(StringLightsArt.WARM, top.bulbRamp[0]));
    }

    @Test
    void bulbsAnchorToTheRasterAlongTheString() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        overlay.update(0.016F);

        LightString top = overlay.getString(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        assertTrue(top.rasterCount > 400);
        int previous = -1;
        for (int i = 0; i < top.bulbCount; i++) {
            int point = top.bulbPoint[i];
            assertTrue(point > previous, "bulbs follow the string in order");
            assertTrue(top.rasterT[point] >= top.bulbT[i], "bulb " + i + " hangs at or past its spot");
            assertTrue((point == 0) || (top.rasterT[point - 1] < top.bulbT[i]), "bulb " + i + " hangs at the first point past its spot");
            previous = point;
        }
    }

    @Test
    void rasterIsAConnectedOnePixelLineWithoutCorners() {
        StringLightsDecorationOverlay overlay = layoutOverlay();
        for (int frame = 0; frame < 40; frame++) {
            overlay.update(0.05F);
            for (StringLightsPosition position : List.of(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)) {
                LightString string = overlay.getString(position);
                for (int i = 1; i < string.rasterCount; i++) {
                    int dx = Math.abs(string.rasterX[i] - string.rasterX[i - 1]);
                    int dy = Math.abs(string.rasterY[i] - string.rasterY[i - 1]);
                    assertTrue((dx <= 1) && (dy <= 1) && ((dx + dy) > 0), "step " + i + " of " + position);
                }
            }
        }
    }

    @Test
    void cornerRemovalMatchesRemovingPointsOneByOne() {
        java.util.Random random = new java.util.Random(42);
        for (int run = 0; run < 300; run++) {
            int count = 2 + random.nextInt(40);
            int[] xs = new int[count];
            int[] ys = new int[count];
            float[] ts = new float[count];
            int x = 0;
            int y = 0;
            for (int i = 0; i < count; i++) {
                // Steps of a rastered curve: right, down or up, now and then diagonal
                int step = random.nextInt(4);
                if (i > 0) {
                    x += (step != 1) ? 1 : 0;
                    y += (step == 1) ? 1 : ((step == 2) ? -1 : 0);
                }
                xs[i] = x;
                ys[i] = y;
                ts[i] = i;
            }
            List<int[]> expected = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) expected.add(new int[]{xs[i], ys[i], i});
            // The mockup removes corner points with splice while it walks forward
            for (int i = 1; i < expected.size() - 1; i++) {
                int[] a = expected.get(i - 1);
                int[] c = expected.get(i + 1);
                if ((Math.abs(a[0] - c[0]) == 1) && (Math.abs(a[1] - c[1]) == 1)) expected.remove(i);
            }
            int remaining = LightString.removeCorners(xs, ys, ts, count);
            assertEquals(expected.size(), remaining, "run " + run);
            for (int i = 0; i < remaining; i++) {
                assertEquals(expected.get(i)[2], (int)ts[i], "run " + run + " point " + i);
                assertEquals(expected.get(i)[0], xs[i]);
                assertEquals(expected.get(i)[1], ys[i]);
            }
        }
    }

    @Test
    void bulbCountsAndScalesResolveLikeTheOptionsDescribe() {
        assertAll(() -> assertEquals(20, LightString.resolveBulbCount(0, 480.0F, 24.0F)), () -> assertEquals(1, LightString.resolveBulbCount(0, 5.0F, 24.0F)), () -> assertEquals(0, LightString.resolveBulbCount(0, 0.0F, 24.0F)), () -> assertEquals(LightString.MAX_BULBS, LightString.resolveBulbCount(0, 100000.0F, 24.0F)), () -> assertEquals(5, LightString.resolveBulbCount(5, 1000.0F, 24.0F)), () -> assertEquals(LightString.MAX_BULBS, LightString.resolveBulbCount(LightString.MAX_BULBS + 1, 300.0F, 24.0F)));
        assertAll(() -> assertEquals(2, LightString.resolveBulbScale(null, 2)), () -> assertEquals(0, LightString.resolveBulbScale(0.0F, 2)), () -> assertEquals(0, LightString.resolveBulbScale(-3.0F, 1)), () -> assertEquals(1, LightString.resolveBulbScale(0.2F, 1)), () -> assertEquals(2, LightString.resolveBulbScale(1.5F, 1)), () -> assertEquals(4, LightString.resolveBulbScale(2.0F, 2)), () -> assertEquals(4, LightString.resolveBulbScale(10.0F, 3)));
        List<Integer> pattern = Arrays.asList(1, null, 3);
        assertAll(() -> assertEquals(1, LightString.resolvePatternEntry(pattern, 0)), () -> assertNull(LightString.resolvePatternEntry(pattern, 1)), () -> assertEquals(1, LightString.resolvePatternEntry(pattern, 3)), () -> assertEquals(3, LightString.resolvePatternEntry(pattern, 8)), () -> assertNull(LightString.resolvePatternEntry(List.<Integer>of(), 4)));
    }

    @Test
    void lightSizesKeepTheIndexOfEveryEntry() {
        assertAll(() -> assertEquals(Arrays.asList(1.0F, 2.5F, null, null, 0.0F), StringLightsDecorationOverlay.parseLightSizes("1, 2.5,,abc, 0")), () -> assertEquals(Arrays.asList(null, 3.0F), StringLightsDecorationOverlay.parseLightSizes(",3")), () -> assertEquals(List.of(1.0F, 2.0F), StringLightsDecorationOverlay.parseLightSizes("1,2,")), () -> assertEquals(Arrays.asList(null, null), StringLightsDecorationOverlay.parseLightSizes("NaN, Infinity")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightSizes("")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightSizes("  ")));
    }

    @Test
    void lightColorsKeepTheIndexOfEveryEntry() {
        assertAll(() -> assertEquals(Arrays.asList(0xFFFF0000, 0x8000FF00, null, null, null), StringLightsDecorationOverlay.parseLightColors("#FF0000, 00FF0080, , nope, #12345")), () -> assertEquals(List.of(0xFFABCDEF), StringLightsDecorationOverlay.parseLightColors(" #abcdef ")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightColors("")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightColors("  ")));
    }

    /**
     * An overlay laid out on a screen of the mockup's size, with its settings read.
     */
    private static StringLightsDecorationOverlay layoutOverlay() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();
        overlay.onResize(480, 270);
        overlay.update(0.0F);
        return overlay;
    }

    private static PropertyContainer legacyContainer() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "string_lights");
        return serialized;
    }

    private static StringLightsDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<StringLightsDecorationOverlay> instances = new StringLightsDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
