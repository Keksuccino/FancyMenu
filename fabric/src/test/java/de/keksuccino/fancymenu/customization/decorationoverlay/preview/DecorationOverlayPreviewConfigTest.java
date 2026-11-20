package de.keksuccino.fancymenu.customization.decorationoverlay.preview;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorationOverlayPreviewConfigTest {

    private static final String OUTPUT = Path.of(System.getProperty("java.io.tmpdir"), "overlay-preview").toAbsolutePath().toString();

    @Test
    void onlyRunsWhenAnOverlayIsAskedFor() {
        assertFalse(DecorationOverlayPreviewConfig.isRequested(key -> null));
        assertFalse(DecorationOverlayPreviewConfig.isRequested(Map.of(DecorationOverlayPreviewConfig.ENV_OVERLAY, " ")::get));
        assertTrue(DecorationOverlayPreviewConfig.isRequested(Map.of(DecorationOverlayPreviewConfig.ENV_OVERLAY, "snowfall")::get));
    }

    @Test
    void defaultsMatchTheMockupStage() {
        DecorationOverlayPreviewConfig config = DecorationOverlayPreviewConfig.read(environment(Map.of()));
        assertEquals("snowfall", config.overlayType());
        assertNull(config.layers());
        assertEquals(480, config.guiWidth());
        assertEquals(270, config.guiHeight());
        assertEquals(4, config.guiScale());
        assertEquals(List.of(3.0F), config.shotSeconds());
        assertEquals(DecorationOverlayPreviewConfig.DEFAULT_BACKGROUND, config.backgroundColor());
        assertFalse(config.fullResolutionShots());
        assertNull(config.seed());
        assertEquals("snowfall_default", config.namePrefix());
    }

    @Test
    void readsAllSettings() {
        Map<String, String> values = new HashMap<>();
        values.put(DecorationOverlayPreviewConfig.ENV_LAYERS, "ONLY_BACKGROUND");
        values.put(DecorationOverlayPreviewConfig.ENV_PROPERTIES, "amount=200; size=2 ;look=simple;");
        values.put(DecorationOverlayPreviewConfig.ENV_SHOTS, "5,1.5, 3");
        values.put(DecorationOverlayPreviewConfig.ENV_INPUT, "4:click_widget:mc_titlescreen_options_button;1:move:240,135;2:click:10.5,20;5:title");
        values.put(DecorationOverlayPreviewConfig.ENV_SIZE, "640x360");
        values.put(DecorationOverlayPreviewConfig.ENV_GUI_SCALE, "3");
        values.put(DecorationOverlayPreviewConfig.ENV_BACKGROUND, "panorama");
        values.put(DecorationOverlayPreviewConfig.ENV_SHOT_SCALE, "full");
        values.put(DecorationOverlayPreviewConfig.ENV_SEED, "42");

        DecorationOverlayPreviewConfig config = DecorationOverlayPreviewConfig.read(environment(values));

        assertEquals("only_background", config.layers());
        assertEquals(Map.of("amount", "200", "size", "2", "look", "simple"), config.properties());
        assertEquals(List.of(1.5F, 3.0F, 5.0F), config.shotSeconds());
        assertEquals(4, config.inputs().size());
        DecorationOverlayPreviewConfig.Input move = config.inputs().get(0);
        assertEquals(DecorationOverlayPreviewConfig.InputAction.MOVE, move.action());
        assertEquals(240.0F, move.x());
        assertEquals(135.0F, move.y());
        assertEquals(10.5F, config.inputs().get(1).x());
        assertEquals("mc_titlescreen_options_button", config.inputs().get(2).argument());
        assertEquals(DecorationOverlayPreviewConfig.InputAction.TITLE, config.inputs().get(config.inputs().size() - 1).action());
        assertEquals(640, config.guiWidth());
        assertEquals(360, config.guiHeight());
        assertEquals(3, config.guiScale());
        assertNull(config.backgroundColor());
        assertTrue(config.fullResolutionShots());
        assertEquals(42, config.seed());
        assertEquals(5.0F, config.getEndSeconds());
    }

    @Test
    void readsPropertyChangesWhileTheOverlayRuns() {
        DecorationOverlayPreviewConfig config = DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_INPUT, "4:set:grass=false;8:set:grass = true")));

        assertEquals(2, config.inputs().size());
        assertEquals(DecorationOverlayPreviewConfig.InputAction.SET, config.inputs().get(0).action());
        assertEquals("grass=false", config.inputs().get(0).argument());
        assertEquals(8.0F, config.inputs().get(config.inputs().size() - 1).seconds());
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_INPUT, "1:set:grass"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_INPUT, "1:set:=true"))));
    }

    @Test
    void explainsBrokenSettings() {
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(Map.of(DecorationOverlayPreviewConfig.ENV_OVERLAY, "snowfall")::get));
        // A relative folder would end up in the game directory
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(Map.of(DecorationOverlayPreviewConfig.ENV_OVERLAY, "snowfall", DecorationOverlayPreviewConfig.ENV_OUTPUT, "screenshots")::get));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_LAYERS, "sideways"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_INPUT, "1:jump"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_INPUT, "1:click:10"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_SHOTS, "-1"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_SIZE, "480"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_BACKGROUND, "blue"))));
        assertThrows(IllegalArgumentException.class, () -> DecorationOverlayPreviewConfig.read(environment(Map.of(DecorationOverlayPreviewConfig.ENV_PROPERTIES, "amount"))));
    }

    private static UnaryOperator<String> environment(Map<String, String> extra) {
        Map<String, String> values = new HashMap<>(extra);
        values.putIfAbsent(DecorationOverlayPreviewConfig.ENV_OVERLAY, "snowfall");
        values.putIfAbsent(DecorationOverlayPreviewConfig.ENV_OUTPUT, OUTPUT);
        return values::get;
    }

}
