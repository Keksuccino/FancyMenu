package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay.StringLightsPosition;
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
            assertAll(name, () -> assertNotNull(overlay.getProperty(keyBase)), () -> assertNotNull(overlay.getProperty(keyBase + "_christmas_mode")), () -> assertNotNull(overlay.getProperty(keyBase + "_color_hex")), () -> assertEquals("#FFD27A", overlay.getProperty(keyBase + "_color_hex").getDefault()), () -> assertEquals(false, overlay.getProperty(keyBase + "_christmas_mode").getDefault()));
        }
        assertAll(() -> assertEquals(true, overlay.getProperty("string_lights_left_center_to_top_center").getDefault()), () -> assertEquals(true, overlay.getProperty("string_lights_right_center_to_top_center").getDefault()), () -> assertEquals(true, overlay.getProperty("string_lights_top_left_to_top_right").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_left_to_top_center").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_right_to_top_center").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_bottom_left_to_bottom_right").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_loose_left_top").getDefault()), () -> assertEquals(false, overlay.getProperty("string_lights_loose_right_top").getDefault()));
    }

    @Test
    void legacyLayoutLoadsWithDefaultsForNewSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "string_lights");
        serialized.putProperty("string_lights_left_center_to_top_center", "false");
        serialized.putProperty("string_lights_loose_left_top", "true");
        serialized.putProperty("string_lights_loose_left_top_christmas_mode", "true");
        serialized.putProperty("string_lights_loose_left_top_color_hex", "#FF0000");

        StringLightsDecorationOverlay overlay = deserialize(serialized);
        overlay.updateOverlay();

        StringLightsOverlay renderer = overlay.overlay;
        assertAll(() -> assertFalse(renderer.isPositionEnabled(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)), () -> assertTrue(renderer.isPositionEnabled(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertTrue(renderer.isPositionEnabled(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, renderer.getPositionBulbCount(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(StringLightsOverlay.DEFAULT_STRING_COLOR, renderer.getPositionStringColor(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(List.of(), renderer.getPositionBulbSizes(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(List.of(), renderer.getPositionBulbColors(StringLightsPosition.LOOSE_LEFT_TOP)));
    }

    @Test
    void newSettingsSurviveSerializationRoundTrip() {
        StringLightsDecorationOverlay original = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = original.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightCount.set(12);
        topString.stringColor.set("#FF000080");
        topString.lightSizes.set("1.0, 2.0");
        topString.lightColors.set("#00FF00, , #0000FF");

        StringLightsDecorationOverlay loaded = deserialize(new StringLightsDecorationOverlayBuilder()._serialize(original));
        StringLightsDecorationOverlay.StringLightProperties loadedTopString = loaded.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);

        assertAll(() -> assertEquals(12, loadedTopString.lightCount.getInteger()), () -> assertEquals("#FF000080", loadedTopString.stringColor.get()), () -> assertEquals("1.0, 2.0", loadedTopString.lightSizes.get()), () -> assertEquals("#00FF00, , #0000FF", loadedTopString.lightColors.get()), () -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, loaded.stringLights.get(StringLightsPosition.LOOSE_LEFT_TOP).lightCount.getInteger()));
    }

    @Test
    void settingsOfOneStringAreAppliedOnlyToItsPosition() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightCount.set(12);
        topString.stringColor.set("#FF000080");
        topString.lightSizes.set("1.5, 0");
        topString.lightColors.set("#00FF00");

        overlay.updateOverlay();

        StringLightsOverlay renderer = overlay.overlay;
        assertAll(() -> assertEquals(12, renderer.getPositionBulbCount(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(0x80FF0000, renderer.getPositionStringColor(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(List.of(1.5F, 0.0F), renderer.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(List.of(0xFF00FF00), renderer.getPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, renderer.getPositionBulbCount(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)), () -> assertEquals(StringLightsOverlay.DEFAULT_STRING_COLOR, renderer.getPositionStringColor(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)), () -> assertEquals(List.of(), renderer.getPositionBulbSizes(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)), () -> assertEquals(List.of(), renderer.getPositionBulbColors(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)));
    }

    @Test
    void changedAndClearedPatternsReachTheOverlay() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        StringLightsOverlay renderer = overlay.overlay;

        topString.lightSizes.set("2");
        topString.lightColors.set("#00FF00");
        overlay.updateOverlay();
        topString.lightSizes.set("3, 4");
        topString.lightColors.set("#0000FF");
        overlay.updateOverlay();

        assertAll(() -> assertEquals(List.of(3.0F, 4.0F), renderer.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(List.of(0xFF0000FF), renderer.getPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)));

        topString.lightSizes.set("");
        topString.lightColors.set("   ");
        overlay.updateOverlay();

        assertAll(() -> assertEquals(List.of(), renderer.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(List.of(), renderer.getPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)));
    }

    @Test
    void outOfRangeSettingsGetClamped() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        StringLightsDecorationOverlay.StringLightProperties looseString = overlay.stringLights.get(StringLightsPosition.LOOSE_LEFT_TOP);
        topString.lightCount.set(StringLightsOverlay.MAX_CUSTOM_BULB_COUNT + 50);
        looseString.lightCount.set(-4);
        topString.lightSizes.set("25, -1");

        overlay.updateOverlay();

        StringLightsOverlay renderer = overlay.overlay;
        assertAll(() -> assertEquals(StringLightsOverlay.MAX_CUSTOM_BULB_COUNT, renderer.getPositionBulbCount(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, renderer.getPositionBulbCount(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(List.of(StringLightsOverlay.MAX_BULB_SIZE_MULTIPLIER, StringLightsOverlay.MIN_BULB_SIZE_MULTIPLIER), renderer.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)));
    }

    @Test
    void patternsResolvePlaceholdersBeforeSplittingEntries() {
        StringLightsDecorationOverlay overlay = new StringLightsDecorationOverlay();
        StringLightsDecorationOverlay.StringLightProperties topString = overlay.stringLights.get(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        topString.lightSizes.set(TestPlaceholders.echo("1.5, 2"));
        topString.lightColors.set(TestPlaceholders.echo("#FF0000, #00FF00"));
        topString.lightCount.setManualInput(TestPlaceholders.echo("7"));

        overlay.updateOverlay();

        StringLightsOverlay renderer = overlay.overlay;
        assertAll(() -> assertEquals(List.of(1.5F, 2.0F), renderer.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(List.of(0xFFFF0000, 0xFF00FF00), renderer.getPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(7, renderer.getPositionBulbCount(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)));
    }

    @Test
    void lightSizesKeepTheIndexOfEveryEntry() {
        assertAll(() -> assertEquals(Arrays.asList(1.0F, 2.5F, null, null, 0.0F), StringLightsDecorationOverlay.parseLightSizes("1, 2.5,,abc, 0")), () -> assertEquals(Arrays.asList(null, 3.0F), StringLightsDecorationOverlay.parseLightSizes(",3")), () -> assertEquals(List.of(1.0F, 2.0F), StringLightsDecorationOverlay.parseLightSizes("1,2,")), () -> assertEquals(Arrays.asList(null, null), StringLightsDecorationOverlay.parseLightSizes("NaN, Infinity")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightSizes("")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightSizes("  ")));
    }

    @Test
    void lightColorsKeepTheIndexOfEveryEntry() {
        assertAll(() -> assertEquals(Arrays.asList(0xFFFF0000, 0x8000FF00, null, null, null), StringLightsDecorationOverlay.parseLightColors("#FF0000, 00FF0080, , nope, #12345")), () -> assertEquals(List.of(0xFFABCDEF), StringLightsDecorationOverlay.parseLightColors(" #abcdef ")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightColors("")), () -> assertEquals(List.of(), StringLightsDecorationOverlay.parseLightColors("  ")));
    }

    private static StringLightsDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<StringLightsDecorationOverlay> instances = new StringLightsDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
