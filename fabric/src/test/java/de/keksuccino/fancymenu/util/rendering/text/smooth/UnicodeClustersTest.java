package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeClustersTest {

    @Test
    void zeroWidthJoinerSequenceIsOneEmojiCluster() {
        int[] family = "👨‍👩‍👧x".codePoints().toArray();
        assertEquals(5, UnicodeClusters.clusterEnd(family, 0, family.length));
        assertTrue(UnicodeClusters.isEmojiCluster(family, 0, 5));
        assertEquals(6, UnicodeClusters.clusterEnd(family, 5, family.length));
    }

    @Test
    void regionalIndicatorsPairUpIntoFlags() {
        int[] flags = "🇩🇪🇯🇵🇫".codePoints().toArray();
        assertEquals(2, UnicodeClusters.clusterEnd(flags, 0, flags.length));
        assertEquals(4, UnicodeClusters.clusterEnd(flags, 2, flags.length));
        // A lone regional indicator at the end stays a cluster of its own.
        assertEquals(5, UnicodeClusters.clusterEnd(flags, 4, flags.length));
        assertTrue(UnicodeClusters.isEmojiCluster(flags, 0, 2));
    }

    @Test
    void modifiersKeycapsAndCombiningMarksExtendTheirBase() {
        int[] thumbs = "👍🏽".codePoints().toArray();
        assertEquals(2, UnicodeClusters.clusterEnd(thumbs, 0, thumbs.length));
        int[] keycap = "1️⃣".codePoints().toArray();
        assertEquals(3, UnicodeClusters.clusterEnd(keycap, 0, keycap.length));
        assertTrue(UnicodeClusters.isEmojiCluster(keycap, 0, 3));
        int[] accented = "éa".codePoints().toArray();
        assertEquals(2, UnicodeClusters.clusterEnd(accented, 0, accented.length));
        assertFalse(UnicodeClusters.isEmojiCluster(accented, 0, 2));
    }

    @Test
    void presentationSelectorsDecideTextOrEmojiStyle() {
        int[] heart = {0x2764};
        int[] emojiHeart = {0x2764, UnicodeClusters.EMOJI_PRESENTATION_SELECTOR};
        int[] textSmiley = {0x1F600, UnicodeClusters.TEXT_PRESENTATION_SELECTOR};
        int[] smiley = {0x1F600};
        assertFalse(UnicodeClusters.isEmojiCluster(heart, 0, 1));
        assertTrue(UnicodeClusters.isEmojiCluster(emojiHeart, 0, 2));
        assertFalse(UnicodeClusters.isEmojiCluster(textSmiley, 0, 2));
        assertTrue(UnicodeClusters.isEmojiCluster(smiley, 0, 1));
    }

    @Test
    void lineBreaksAreAlwaysSingleClusters() {
        int[] text = {'\n', 0x0301};
        assertEquals(1, UnicodeClusters.clusterEnd(text, 0, text.length));
    }

}
