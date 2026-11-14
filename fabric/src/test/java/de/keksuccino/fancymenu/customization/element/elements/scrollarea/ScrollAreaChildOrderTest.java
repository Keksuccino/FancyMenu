package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.junit.jupiter.api.Test;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollAreaChildOrderTest {

    /** X, Y, width and height of every box, one box after another. */
    private static int[] boxes(int... values) {
        return values;
    }

    @Test
    void boxesGetReadLineByLineLikeText() {
        // Two lines of two, given from the bottom right to the top left
        int[] boxes = boxes(
                60, 50, 40, 20,
                0, 52, 40, 20,
                60, 3, 40, 20,
                0, 0, 40, 20);
        assertArrayEquals(new int[]{3, 2, 1, 0}, ScrollAreaEditorElement.orderByPosition(boxes, false));
    }

    @Test
    void anAreaThatScrollsSidewaysReadsColumnByColumn() {
        int[] boxes = boxes(
                0, 50, 40, 20,
                60, 0, 40, 20,
                2, 0, 40, 20,
                60, 50, 40, 20);
        assertArrayEquals(new int[]{2, 0, 1, 3}, ScrollAreaEditorElement.orderByPosition(boxes, true));
    }

    @Test
    void aBoxIsInTheLineOfTheBoxThatStartedIt() {
        // The second box shares a line with the first one and with the third one, which do not share one with each other
        int[] boxes = boxes(
                0, 0, 40, 100,
                100, 40, 40, 20,
                50, 70, 40, 100);
        assertArrayEquals(new int[]{0, 1, 2}, ScrollAreaEditorElement.orderByPosition(boxes, false));
    }

    @Test
    void boxesInTheSamePlaceKeepTheOrderTheyHad() {
        int[] boxes = boxes(
                10, 10, 40, 20,
                10, 10, 40, 20,
                10, 10, 40, 20);
        assertArrayEquals(new int[]{0, 1, 2}, ScrollAreaEditorElement.orderByPosition(boxes, false));
        assertEquals(0, ScrollAreaEditorElement.orderByPosition(boxes(), false).length);
    }

    @Test
    void manyBoxesThatOverlapInEveryWayStillGetAnOrder() {
        // Sorting throws for an order that contradicts itself, which shows from 32 boxes on
        Random random = new Random(7);
        for (int round = 0; round < 200; round++) {
            int count = 32 + random.nextInt(40);
            int[] boxes = new int[count * 4];
            for (int i = 0; i < count; i++) {
                boxes[i * 4] = random.nextInt(300);
                boxes[(i * 4) + 1] = random.nextInt(300);
                boxes[(i * 4) + 2] = 5 + random.nextInt(120);
                boxes[(i * 4) + 3] = 5 + random.nextInt(120);
            }
            int[] order = ScrollAreaEditorElement.orderByPosition(boxes, (round % 2) == 0);
            boolean[] seen = new boolean[count];
            for (int index : order) {
                seen[index] = true;
            }
            for (boolean present : seen) {
                assertEquals(true, present);
            }
        }
    }

}
