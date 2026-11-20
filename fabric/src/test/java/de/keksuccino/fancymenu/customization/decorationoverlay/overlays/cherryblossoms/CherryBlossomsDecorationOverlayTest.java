package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CherryBlossomsDecorationOverlayTest {

    @Test
    void builderAndOptionsUseTheirKeys() {
        CherryBlossomsDecorationOverlayBuilder builder = new CherryBlossomsDecorationOverlayBuilder();
        CherryBlossomsDecorationOverlay overlay = builder.buildDefaultInstance();

        assertAll(() -> assertEquals("cherry_blossoms", builder.getIdentifier()), () -> assertEquals(DecorationOverlayLook.DETAILED, overlay.look.get()), () -> assertTrue(overlay.leftBranch.getBoolean()), () -> assertTrue(overlay.rightBranch.getBoolean()), () -> assertEquals(100, overlay.amount.getPercent()), () -> assertNotNull(overlay.getProperty("cherry_blossoms_left_branch")), () -> assertNotNull(overlay.getProperty("cherry_blossoms_right_branch")), () -> assertNotNull(overlay.getProperty("look")), () -> assertNotNull(overlay.getProperty("amount")));
    }

    @Test
    void optionsSurviveSerializationRoundTrip() {
        CherryBlossomsDecorationOverlayBuilder builder = new CherryBlossomsDecorationOverlayBuilder();
        CherryBlossomsDecorationOverlay original = builder.buildDefaultInstance();
        original.look.property.set(DecorationOverlayLook.SIMPLE);
        original.leftBranch.set(false);
        original.amount.property.set(250);

        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(builder._serialize(original));
        List<CherryBlossomsDecorationOverlay> loaded = builder._deserializeAll(layout);

        assertEquals(1, loaded.size());
        CherryBlossomsDecorationOverlay overlay = loaded.get(0);
        assertAll(() -> assertEquals(DecorationOverlayLook.SIMPLE, overlay.look.get()), () -> assertFalse(overlay.leftBranch.getBoolean()), () -> assertTrue(overlay.rightBranch.getBoolean()), () -> assertEquals(250, overlay.amount.getPercent()));
    }

    @Test
    void branchesGrowExactlyLikeTheMockupsBranches() {
        CherryBlossomBranch left = new CherryBlossomBranch(901, false);
        CherryBlossomBranch right = new CherryBlossomBranch(977, true);
        left.build(480, 270, 1.0, false);
        right.build(480, 270, 1.0, false);

        // Counts and first tip of the mockup's sakuraBranch(901, false) and sakuraBranch(977, true) on its 480 x 270 stage
        assertAll(() -> assertEquals(624, left.barkPixelCount()), () -> assertEquals(137, left.clusterCount()), () -> assertEquals(43, left.tipCount()), () -> assertEquals(28.4022F, left.tipX(0), 0.0001F), () -> assertEquals(17.4581F, left.tipY(0), 0.0001F), () -> assertEquals(748, right.barkPixelCount()), () -> assertEquals(192, right.clusterCount()), () -> assertEquals(40, right.tipCount()));
    }

    @Test
    void onlyTipsOnTheScreenLetPetalsGo() {
        CherryBlossomBranch right = new CherryBlossomBranch(977, true);
        right.build(480, 270, 1.0, false);
        for (int i = 0; i < right.tipCount(); i++) {
            assertTrue((right.tipX(i) >= 0.0F) && (right.tipX(i) < 480.0F) && (right.tipY(i) >= 0.0F), "tip " + i);
        }
    }

    @Test
    void carpetsHoldTwoPetalsOnElementsAndThreeOnTheFloor() {
        PixelRandom random = new PixelRandom(1);
        PixelSurfaces<PetalCarpet> surfaces = new PixelSurfaces<>(surface -> new PetalCarpet(surface, random, new PixelCanvas("test_carpet")));
        surfaces.update(List.of(new CollisionAreaBounds(100, 80, 60, 20)), 480, 270);
        PetalCarpet element = surfaces.elements().get(0).state();
        PetalCarpet floor = surfaces.floor().state();
        assertNotNull(element);
        assertNotNull(floor);

        for (int i = 0; i < 5; i++) {
            element.add(10);
            floor.add(10);
        }

        assertAll(() -> assertTrue(element.onElement), () -> assertFalse(floor.onElement), () -> assertEquals(2, element.height(10)), () -> assertEquals(3, floor.height(10)), () -> assertEquals(60, element.width), () -> assertEquals(79, element.restY), () -> assertEquals(269, floor.restY), () -> assertFalse(element.add(-1)), () -> assertFalse(element.add(60)));
        assertTrue(element.remove(10));
        assertTrue(element.remove(10));
        assertAll(() -> assertEquals(0, element.height(10)), () -> assertFalse(element.remove(10)));
    }

    @Test
    void carpetsThinOutOverTime() {
        PixelRandom random = new PixelRandom(2);
        PixelSurfaces<PetalCarpet> surfaces = new PixelSurfaces<>(surface -> new PetalCarpet(surface, random, new PixelCanvas("test_carpet")));
        surfaces.update(List.of(), 480, 270);
        PetalCarpet floor = surfaces.floor().state();
        assertNotNull(floor);
        int before = total(floor);

        for (int i = 0; i < 60; i++) floor.thin(1.0F, random);

        int after = total(floor);
        assertTrue(after < before, "petals blow away or wither");
        assertTrue(after < before / 2, "after a minute most of the carpet is gone");
    }

    private static int total(PetalCarpet carpet) {
        int sum = 0;
        for (int i = 0; i < carpet.width; i++) sum += carpet.height(i);
        return sum;
    }

    @Test
    void carpetsOfMovedElementsKeepTheirPetals() {
        PixelRandom random = new PixelRandom(3);
        PixelSurfaces<PetalCarpet> surfaces = new PixelSurfaces<>(surface -> new PetalCarpet(surface, random, new PixelCanvas("test_carpet")));
        surfaces.update(List.of(new CollisionAreaBounds(100, 80, 60, 20)), 480, 270);
        PetalCarpet before = surfaces.elements().get(0).state();
        surfaces.update(List.of(new CollisionAreaBounds(103, 80, 60, 20)), 480, 270);
        PetalCarpet after = surfaces.elements().get(0).state();
        assertNotNull(before);
        assertNotNull(after);

        assertTrue(after.canInherit(before));
        after.inherit(before);

        for (int i = 0; i < before.width; i++) assertEquals(before.height(i), after.height(i), "column " + i);
        assertFalse(surfaces.floor().state().canInherit(before), "carpets only take over carpets of the same size");
    }

    @Test
    void layoutWithoutSavedCherryBlossomsGetsDefaults() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "cherry_blossoms");
        serialized.putProperty("show_decoration_overlay", "true");
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);

        CherryBlossomsDecorationOverlay overlay = new CherryBlossomsDecorationOverlayBuilder()._deserializeAll(layout).get(0);

        assertAll(() -> assertEquals(DecorationOverlayLook.DETAILED, overlay.look.get()), () -> assertTrue(overlay.leftBranch.getBoolean()), () -> assertTrue(overlay.rightBranch.getBoolean()));
    }

}
