package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothAtlasPackerTest {

    @Test
    void allocationsStayInsideThePageAndNeverOverlap() {
        SmoothAtlasPacker packer = new SmoothAtlasPacker(256, 256);
        Random random = new Random(42);
        List<int[]> placed = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            int width = 3 + random.nextInt(30);
            int height = 3 + random.nextInt(30);
            long position = packer.allocate(width, height);
            if (position == SmoothAtlasPacker.NO_SPACE) {
                continue;
            }
            int[] rect = {SmoothAtlasPacker.x(position), SmoothAtlasPacker.y(position), width, height};
            assertTrue(rect[0] >= 0 && rect[1] >= 0 && rect[0] + width <= 256 && rect[1] + height <= 256);
            for (int[] other : placed) {
                boolean overlaps = rect[0] < other[0] + other[2] && other[0] < rect[0] + rect[2] && rect[1] < other[1] + other[3] && other[1] < rect[1] + rect[3];
                assertFalse(overlaps);
            }
            placed.add(rect);
        }
        assertTrue(placed.size() > 40);
    }

    @Test
    void fullPagesReportNoSpaceUntilReset() {
        SmoothAtlasPacker packer = new SmoothAtlasPacker(64, 64);
        assertNotEquals(SmoothAtlasPacker.NO_SPACE, packer.allocate(64, 64));
        assertEquals(SmoothAtlasPacker.NO_SPACE, packer.allocate(1, 1));
        packer.reset();
        assertEquals(0L, packer.allocate(10, 10));
    }

    @Test
    void oversizedAndEmptyRequestsAreRejected() {
        SmoothAtlasPacker packer = new SmoothAtlasPacker(64, 64);
        assertEquals(SmoothAtlasPacker.NO_SPACE, packer.allocate(65, 4));
        assertEquals(SmoothAtlasPacker.NO_SPACE, packer.allocate(0, 4));
    }

    @Test
    void similarHeightsShareAShelf() {
        SmoothAtlasPacker packer = new SmoothAtlasPacker(128, 128);
        long first = packer.allocate(10, 14);
        long second = packer.allocate(10, 13);
        assertEquals(SmoothAtlasPacker.y(first), SmoothAtlasPacker.y(second));
        assertEquals(10, SmoothAtlasPacker.x(second));
    }

}
