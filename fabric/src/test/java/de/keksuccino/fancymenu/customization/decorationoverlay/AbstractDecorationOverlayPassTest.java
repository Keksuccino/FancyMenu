package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.RenderFrameCounter;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Runs the render passes of overlays without a game. The overlays here never touch the graphics, so the passes get NULL for it.
 */
class AbstractDecorationOverlayPassTest {

    /** The pass the test runs right now, so the test overlays can record which pass drew a part. */
    private static DecorationOverlayPass currentPass = null;

    @Test
    void backgroundAndForegroundDrawsBackBehindAndTheRestInFront() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND);
        runFrame(overlay);
        assertEquals(List.of("update", "background:BACK", "foreground:ELEMENT", "foreground:FRONT"), overlay.events);
    }

    @Test
    void onlyBackgroundDrawsBackAndFrontBehindTheElements() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.ONLY_BACKGROUND);
        runFrame(overlay);
        assertEquals(List.of("update", "background:BACK", "background:FRONT", "foreground:ELEMENT"), overlay.events);
    }

    @Test
    void onlyForegroundDrawsEverythingInFront() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.ONLY_FOREGROUND);
        runFrame(overlay);
        assertEquals(List.of("update", "foreground:BACK", "foreground:ELEMENT", "foreground:FRONT"), overlay.events);
    }

    @Test
    void theSimulationAdvancesOncePerFrameWhateverPassesRun() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND);
        for (int i = 0; i < 3; i++) runFrame(overlay);
        // The screen of the loading overlay fires its render events twice per frame
        RenderFrameCounter.nextFrame();
        renderPass(overlay, DecorationOverlayPass.BACKGROUND, 0, 0);
        renderPass(overlay, DecorationOverlayPass.FOREGROUND, 0, 0);
        renderPass(overlay, DecorationOverlayPass.BACKGROUND, 0, 0);
        renderPass(overlay, DecorationOverlayPass.FOREGROUND, 0, 0);
        // Screens without a background pass
        for (int i = 0; i < 2; i++) {
            RenderFrameCounter.nextFrame();
            renderPass(overlay, DecorationOverlayPass.FOREGROUND, 0, 0);
        }

        assertEquals(6L, overlay.events.stream().filter("update"::equals).count());
        assertEquals(6L, overlay._getUpdateCount());
        assertEquals(6L, overlay.events.stream().filter("foreground:FRONT"::equals).count());
        assertEquals(4L, overlay.events.stream().filter("background:BACK"::equals).count());
        assertEquals(4L, overlay._getPassCount(DecorationOverlayPass.BACKGROUND));
    }

    @Test
    void theUpdateRunsBeforeTheFirstPartOfTheFrame() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.ONLY_FOREGROUND);
        RenderFrameCounter.nextFrame();
        renderPass(overlay, DecorationOverlayPass.FOREGROUND, 7, 9);
        assertEquals("update", overlay.events.getFirst());
        assertEquals(7, overlay.lastMouseX);
        assertEquals(9, overlay.lastMouseY);
    }

    @Test
    void hiddenOverlaysNeitherUpdateNorDraw() {
        RecordingOverlay overlay = RecordingOverlay.shown(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND);
        overlay.showOverlay.set(false);
        runFrame(overlay);
        assertEquals(List.of(), overlay.events);
    }

    @Test
    void overlaysWithoutPartsDrawTheirContentAsFrontPart() {
        LegacyOverlay overlay = new LegacyOverlay();
        overlay.showOverlay.set(true);
        runFrame(overlay);
        assertEquals(List.of("foreground"), overlay.events);

        overlay.events.clear();
        overlay.layers.set(DecorationOverlayLayers.ONLY_BACKGROUND);
        runFrame(overlay);
        assertEquals(List.of("background"), overlay.events);
    }

    @Test
    void theLayersSettingIsSavedPerOverlay() {
        RecordingOverlay overlay = new RecordingOverlay();
        assertEquals(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND, overlay.getLayers());

        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("decoration_overlay_layers", "only_foreground");
        overlay.layers.deserialize(serialized);
        assertEquals(DecorationOverlayLayers.ONLY_FOREGROUND, overlay.getLayers());

        PropertyContainer written = new PropertyContainer("decoration_overlay");
        overlay.layers.serialize(written);
        assertEquals("only_foreground", written.getValue("decoration_overlay_layers"));

        PropertyContainer unknown = new PropertyContainer("decoration_overlay");
        unknown.putProperty("decoration_overlay_layers", "sideways");
        overlay.layers.deserialize(unknown);
        assertEquals(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND, overlay.getLayers());
        // Old layouts have no Layers setting yet and keep drawing their overlays in front
        overlay.layers.deserialize(new PropertyContainer("decoration_overlay"));
        assertEquals(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND, overlay.getLayers());
    }

    @Test
    void overlaysNeverClaimTheMouse() {
        assertFalse(new RecordingOverlay().isMouseOver(10.0, 10.0));
    }

    @Test
    void sharedOptionsClampTheirValues() {
        RecordingOverlay overlay = new RecordingOverlay();
        DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(overlay);
        DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(overlay);
        DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(overlay, DecorationOverlayLook.SIMPLE);

        assertEquals(1.0F, amount.getFactor());
        amount.property.set(150);
        assertEquals(1.5F, amount.getFactor());
        amount.property.set(10);
        assertEquals(0.25F, amount.getFactor());
        amount.property.set(900);
        assertEquals(3.0F, amount.getFactor());

        assertEquals(1, size.get());
        size.property.set(3);
        assertEquals(3, size.get());
        size.property.set(7);
        assertEquals(3, size.get());

        assertEquals(DecorationOverlayLook.SIMPLE, look.get());
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("look", "detailed");
        look.property.deserialize(serialized);
        assertEquals(DecorationOverlayLook.DETAILED, look.get());
        assertEquals(List.of("amount", "size", "look"), overlay.getPropertyMap().keySet().stream().filter(key -> !key.startsWith("show_") && !key.startsWith("decoration_")).toList());
    }

    private static void runFrame(@NotNull AbstractDecorationOverlay<?> overlay) {
        RenderFrameCounter.nextFrame();
        renderPass(overlay, DecorationOverlayPass.BACKGROUND, 0, 0);
        renderPass(overlay, DecorationOverlayPass.FOREGROUND, 0, 0);
    }

    private static void renderPass(@NotNull AbstractDecorationOverlay<?> overlay, @NotNull DecorationOverlayPass pass, int mouseX, int mouseY) {
        currentPass = pass;
        try {
            overlay._renderPass(pass, null, mouseX, mouseY, 0.0F);
        } finally {
            currentPass = null;
        }
    }

    @NotNull
    private static String currentPassName() {
        return (currentPass == DecorationOverlayPass.BACKGROUND) ? "background" : "foreground";
    }

    private static final class RecordingOverlay extends AbstractDecorationOverlay<RecordingOverlay> {

        private final List<String> events = new ArrayList<>();
        private int lastMouseX = -1;
        private int lastMouseY = -1;

        private static RecordingOverlay shown(@NotNull DecorationOverlayLayers layers) {
            RecordingOverlay overlay = new RecordingOverlay();
            overlay.showOverlay.set(true);
            overlay.layers.set(layers);
            return overlay;
        }

        @Override
        protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        }

        @Override
        protected void updateFrame(float deltaSeconds, int mouseX, int mouseY) {
            this.events.add("update");
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }

        @Override
        protected void renderBackPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.record("BACK");
        }

        @Override
        protected void renderElementPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.record("ELEMENT");
        }

        @Override
        protected void renderFrontPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.record("FRONT");
        }

        private void record(@NotNull String part) {
            this.events.add(currentPassName() + ":" + part);
        }

    }

    private static final class LegacyOverlay extends AbstractDecorationOverlay<LegacyOverlay> {

        private final List<String> events = new ArrayList<>();

        @Override
        protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.events.add(currentPassName());
        }

    }

}
