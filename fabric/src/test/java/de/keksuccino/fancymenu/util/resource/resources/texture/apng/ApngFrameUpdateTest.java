package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApngFrameUpdateTest {

    @Test
    void containedRectangleChangesNothing() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(10, 10, 300, 200);
        update.addRectangle(50, 60, 20, 20);

        assertEquals(1, update.getRectangleCount());
        assertEquals(300L * 200L, update.getPixelCount());
    }

    @Test
    void bigRectanglesFarApartStaySeparate() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(0, 0, 100, 100);
        update.addRectangle(900, 900, 100, 100);

        assertEquals(2, update.getRectangleCount());
        assertEquals(20_000L, update.getPixelCount());
        assertEquals(900, update.getRectangleX(1));
        assertEquals(900, update.getRectangleY(1));
    }

    @Test
    void bigRectanglesCloseTogetherGetMerged() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(0, 0, 100, 100);
        update.addRectangle(10, 0, 100, 100);

        assertEquals(1, update.getRectangleCount());
        assertEquals(0, update.getRectangleX(0));
        assertEquals(110, update.getRectangleWidth(0));
    }

    @Test
    void smallRectanglesGetMergedEvenWhenFarApart() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(0, 0, 2, 2);
        update.addRectangle(40, 40, 2, 2);

        assertEquals(1, update.getRectangleCount());
        assertEquals(42L * 42L, update.getPixelCount());
    }

    @Test
    void thirdRectangleMergesEverything() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(0, 0, 100, 100);
        update.addRectangle(900, 900, 100, 100);
        update.addRectangle(500, 0, 10, 10);

        assertEquals(1, update.getRectangleCount());
        assertEquals(1000, update.getRectangleWidth(0));
        assertEquals(1000, update.getRectangleHeight(0));
    }

    @Test
    void emptyRectanglesGetIgnored() {
        ApngFrameUpdate update = new ApngFrameUpdate(1, 0, 0L);
        update.addRectangle(5, 5, 0, 10);
        update.addRectangle(5, 5, 10, -1);

        assertEquals(0, update.getRectangleCount());
    }

}
