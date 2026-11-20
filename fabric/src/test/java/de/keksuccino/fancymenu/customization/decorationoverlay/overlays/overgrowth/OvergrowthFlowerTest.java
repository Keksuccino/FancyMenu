package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvergrowthFlowerTest {

    private static OvergrowthFlower flower(int position, float birth, boolean spare) {
        return new OvergrowthFlower(position, 0, false, birth, spare, 0, 0, 1, 0.0F, false);
    }

    @Test
    void theFirstFlowersShareTheLowestThirdAndTheSpareOnesTheRest() {
        List<OvergrowthFlower> flowers = new ArrayList<>();
        for (int i = 0; i < 12; i++) flowers.add(flower(i, 0.0F, false));
        for (int i = 0; i < 20; i++) flowers.add(flower(i, 0.0F, true));
        OvergrowthArt.rank(flowers);

        for (OvergrowthFlower flower : flowers) {
            if (flower.spare) {
                assertTrue((flower.rank >= 1.0F / 3.0F) && (flower.rank < 1.0F), "spare rank " + flower.rank);
            } else {
                assertTrue((flower.rank >= 0.0F) && (flower.rank < 1.0F / 3.0F), "first rank " + flower.rank);
            }
        }
    }

    @Test
    void theFlowerAmountLetsFlowersInEvenly() {
        List<OvergrowthFlower> flowers = new ArrayList<>();
        for (int i = 0; i < 40; i++) flowers.add(flower(i, 0.0F, false));
        for (int i = 0; i < 40; i++) flowers.add(flower(i, 0.0F, true));
        OvergrowthArt.rank(flowers);

        // 25 %, 100 % and 300 %: the threshold is a third of the amount
        assertEquals(40, countLetIn(flowers, 1.0F / 3.0F, false));
        assertEquals(0, countLetIn(flowers, 1.0F / 3.0F, true));
        assertEquals(80, countLetIn(flowers, 1.0F, false) + countLetIn(flowers, 1.0F, true));
        int quarter = countLetIn(flowers, 0.25F / 3.0F, false);
        assertTrue((quarter >= 8) && (quarter <= 12), "flowers at 25 %: " + quarter);
        // The golden ratio steps spread them along the list instead of letting in one clump
        int lastIn = -1;
        int widestGap = 0;
        for (int i = 0; i < 40; i++) {
            if (!flowers.get(i).isLetIn(0.25F / 3.0F)) continue;
            if (lastIn >= 0) widestGap = Math.max(widestGap, i - lastIn);
            lastIn = i;
        }
        assertTrue(widestGap <= 6, "widest gap " + widestGap);
    }

    private static int countLetIn(List<OvergrowthFlower> flowers, float threshold, boolean spare) {
        int count = 0;
        for (OvergrowthFlower flower : flowers) {
            if ((flower.spare == spare) && flower.isLetIn(threshold)) count++;
        }
        return count;
    }

    @Test
    void theFirstCheckTakesOverTheStateWithoutRestartingTheFlower() {
        OvergrowthFlower flower = flower(0, 2.0F, false);
        assertEquals(8.0F, flower.age(10.0F, true), 1.0E-4F);
    }

    @Test
    void aFlowerLetInLaterStartsOverAsABud() {
        OvergrowthFlower flower = flower(0, 2.0F, false);
        assertEquals(0.0F, flower.age(10.0F, false));
        float age = flower.age(11.0F, true);
        assertEquals(0.05F, age, 1.0E-4F);
        assertEquals(OvergrowthArt.BLOOM_BUD, OvergrowthArt.bloomStage(age, false));
        assertEquals(OvergrowthArt.BLOOM_FULL, OvergrowthArt.bloomStage(flower.age(14.0F, true), false));
    }

    @Test
    void aClockThatStartedOverGrowsTheFlowerAgain() {
        OvergrowthFlower flower = flower(0, 2.0F, false);
        flower.age(20.0F, true);
        assertTrue(flower.age(1.0F, true) <= 0.0F);
        assertEquals(1.0F, flower.age(3.0F, true), 1.0E-4F);
    }

    @Test
    void bloomsOpenInThreeStagesAndSmallFlowersStaySmall() {
        assertEquals(OvergrowthArt.BLOOM_HIDDEN, OvergrowthArt.bloomStage(0.0F, false));
        assertEquals(OvergrowthArt.BLOOM_BUD, OvergrowthArt.bloomStage(0.5F, false));
        assertEquals(OvergrowthArt.BLOOM_SMALL, OvergrowthArt.bloomStage(1.5F, false));
        assertEquals(OvergrowthArt.BLOOM_FULL, OvergrowthArt.bloomStage(3.0F, false));
        assertEquals(OvergrowthArt.BLOOM_SMALL, OvergrowthArt.bloomStage(30.0F, true));
    }

}
