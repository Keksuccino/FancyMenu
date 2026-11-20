package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bubbles;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureTestScene;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BubbleStreamsTest {

    @Test
    void theMockupStageGetsItsBubblesSpecksAndStreams() {
        CreatureTestScene test = new CreatureTestScene(3, CreatureTestScene.MOCKUP_BUTTONS);
        BubbleStreams bubbles = new BubbleStreams(test.scene);
        test.frame(bubbles, CreatureTestScene.FRAME);
        assertEquals(34, permanent(bubbles));
        assertEquals(40, bubbles.speckCount());
        assertEquals(4, bubbles.streamCount());
        float[] mockup = {60.0F, 150.0F, 330.0F, 420.0F};
        for (int i = 0; i < 4; i++) assertEquals(mockup[i], bubbles.streamX(i), 10.0F);

        // The streams fill the screen up to their limit and stay there
        test.run(bubbles, 30.0F);
        assertTrue(bubbles.bubbleCount() <= bubbles.streamLimit());
        assertTrue(bubbles.bubbleCount() > 60);
    }

    @Test
    void widerScreensGetMoreStreamsAndBubbles() {
        CreatureTestScene test = new CreatureTestScene(5, List.of());
        test.setSize(960, 540);
        BubbleStreams bubbles = new BubbleStreams(test.scene);
        test.frame(bubbles, CreatureTestScene.FRAME);
        assertEquals(8, bubbles.streamCount());
        assertEquals(136, permanent(bubbles));
        assertEquals(160, bubbles.speckCount());
        for (int i = 0; i < bubbles.streamCount(); i++) assertTrue((bubbles.streamX(i) > 0.0F) && (bubbles.streamX(i) < 960.0F));

        test.setSize(480, 270);
        test.frame(bubbles, CreatureTestScene.FRAME);
        assertEquals(4, bubbles.streamCount());
        assertEquals(34, permanent(bubbles));
        assertEquals(40, bubbles.speckCount());
    }

    private static int permanent(BubbleStreams bubbles) {
        int count = 0;
        for (int i = 0; i < bubbles.bubbleCount(); i++) {
            if (!bubbles.bubble(i).temporary) count++;
        }
        return count;
    }

    @Test
    void bubblesThatHitAnElementPopOrSlideAlongItAndEscapeAtTheNearerEdge() {
        CollisionAreaBounds box = new CollisionAreaBounds(100, 120, 280, 18);
        CreatureTestScene test = new CreatureTestScene(7, List.of(box));
        BubbleStreams bubbles = new BubbleStreams(test.scene);
        test.frame(bubbles, CreatureTestScene.FRAME);
        PixelSurfaces.Surface<?> element = test.surfaces.elements().get(0);
        Map<BubbleStreams.Bubble, float[]> before = new IdentityHashMap<>();
        int pops = 0;
        int slides = 0;
        int escapes = 0;
        for (int frame = 0; frame < 90 * 60; frame++) {
            before.clear();
            for (int i = 0; i < bubbles.bubbleCount(); i++) {
                BubbleStreams.Bubble u = bubbles.bubble(i);
                before.put(u, new float[]{u.y, (u.slide != null) ? 1.0F : 0.0F, u.pop});
            }
            test.frame(bubbles, CreatureTestScene.FRAME);
            for (int i = 0; i < bubbles.bubbleCount(); i++) {
                BubbleStreams.Bubble u = bubbles.bubble(i);
                float[] old = before.get(u);
                if (old == null) continue;
                int size = BubbleStreams.SIZES[u.kind];
                boolean wasRising = (old[1] == 0.0F) && (old[2] < 0.0F);
                boolean wasSliding = old[1] != 0.0F;
                if (u.slide != null) {
                    assertSame(element, u.slide);
                    assertTrue(u.onElement, "a bubble caught under an element has to be element content");
                    assertEquals(element.bottom(), u.y, 1.0E-4F);
                    assertEquals(0.0F, u.amplitude);
                    if (wasRising) {
                        slides++;
                        float center = u.x + size / 2.0F;
                        assertEquals((center < element.x() + element.width() / 2.0F) ? -1 : 1, u.slideDir, "it has to slide to the nearer edge");
                    }
                } else if (wasSliding && (u.pop < 0.0F)) {
                    escapes++;
                    assertFalse(u.onElement, "an escaped bubble is free again");
                    assertTrue((u.x + size <= element.x()) || (u.x >= element.right()), "it left the element before its edge");
                } else if (wasRising && (u.pop >= 0.0F) && (old[0] >= element.bottom())) {
                    pops++;
                    assertTrue(u.onElement, "the pop of a bubble that hit an element belongs to the element");
                } else if (wasRising && (u.pop < 0.0F) && (old[0] >= element.bottom()) && (u.y < element.bottom())) {
                    float center = u.x + (float)Math.sin(u.phase) * u.amplitude + size / 2.0F;
                    assertTrue((center < element.x()) || (center >= element.right()), "a bubble went through the element");
                }
                if ((u.slide == null) && (u.pop < 0.0F)) assertFalse(u.onElement, "free bubbles stay front content");
            }
        }
        assertTrue(slides > 20, "slides: " + slides);
        assertTrue(escapes > 10, "escapes: " + escapes);
        float popShare = pops / (float)(pops + slides);
        assertTrue((popShare > 0.15F) && (popShare < 0.45F), "pops: " + pops + ", slides: " + slides);
    }

    @Test
    void aCaughtBubblePoppedByAClickStaysWithItsElement() {
        CreatureTestScene test = new CreatureTestScene(13, List.of(new CollisionAreaBounds(100, 120, 280, 18)));
        BubbleStreams bubbles = new BubbleStreams(test.scene);
        BubbleStreams.Bubble caught = null;
        for (int frame = 0; (frame < 60 * 60) && (caught == null); frame++) {
            test.frame(bubbles, CreatureTestScene.FRAME);
            for (int i = 0; i < bubbles.bubbleCount(); i++) {
                if (bubbles.bubble(i).slide != null) caught = bubbles.bubble(i);
            }
        }
        assertTrue(caught != null, "no bubble got caught under the element");
        bubbles.onClick(caught.x, caught.y, true);
        assertTrue(caught.pop >= 0.0F);
        assertTrue(caught.onElement);
    }

    @Test
    void clicksPopNearbyBubblesAndSetSmallOnesFree() {
        CreatureTestScene test = new CreatureTestScene(11, List.of());
        BubbleStreams bubbles = new BubbleStreams(test.scene);
        test.run(bubbles, 5.0F);
        List<BubbleStreams.Bubble> near = new ArrayList<>();
        for (int i = 0; i < bubbles.bubbleCount(); i++) {
            BubbleStreams.Bubble u = bubbles.bubble(i);
            if ((u.pop < 0.0F) && (Math.hypot(u.x - 240.0F, u.y - 150.0F) < BubbleStreams.CLICK_RADIUS)) near.add(u);
        }
        int count = bubbles.bubbleCount();

        bubbles.onClick(240.0F, 150.0F, false);
        assertEquals(count, bubbles.bubbleCount(), "other buttons do nothing");

        bubbles.onClick(240.0F, 150.0F, true);
        for (BubbleStreams.Bubble u : near) {
            assertTrue(u.pop >= 0.0F);
            assertFalse(u.onElement, "pops of free bubbles stay front content");
        }
        assertEquals(count + BubbleStreams.CLICK_BUBBLES, bubbles.bubbleCount());
        for (int i = count; i < bubbles.bubbleCount(); i++) {
            BubbleStreams.Bubble u = bubbles.bubble(i);
            assertEquals(0, u.kind);
            assertTrue(u.temporary);
            assertTrue(Math.abs(u.x - 240.0F) <= 6.0F && Math.abs(u.y - 150.0F) <= 4.0F);
        }

        // After the pop, the bubbles that are always around start again below the screen
        test.run(bubbles, 0.25F);
        for (BubbleStreams.Bubble u : near) {
            if (!u.temporary) assertTrue((u.pop < 0.0F) && (u.y > 260.0F), "popped bubbles start again below the screen");
        }

        for (int i = 0; i < 100; i++) bubbles.onClick(240.0F, 150.0F, true);
        assertTrue(bubbles.bubbleCount() <= bubbles.clickLimit());
    }

}
