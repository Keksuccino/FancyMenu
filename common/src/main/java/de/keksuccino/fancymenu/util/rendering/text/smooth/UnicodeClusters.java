package de.keksuccino.fancymenu.util.rendering.text.smooth;

/**
 * Splits codepoint sequences into user-perceived characters (clusters) and classifies emoji clusters.
 * <p>
 * Clusters keep base characters together with combining marks, variation selectors, emoji modifiers, keycaps, tag sequences,
 * zero-width-joiner sequences and regional indicator pairs (flags). A cluster is always shaped by a single font source and never
 * gets split by line wrapping. The emoji classification is a compact approximation of the Unicode emoji data, so it stays
 * available on every Java version the mod supports (Java 17 has no {@code Character.isExtendedPictographic}).
 * </p>
 */
final class UnicodeClusters {

    static final int ZERO_WIDTH_JOINER = 0x200D;
    static final int TEXT_PRESENTATION_SELECTOR = 0xFE0E;
    static final int EMOJI_PRESENTATION_SELECTOR = 0xFE0F;
    static final int COMBINING_KEYCAP = 0x20E3;

    private UnicodeClusters() {
    }

    /**
     * Returns the exclusive end index of the cluster that starts at {@code start}.
     */
    static int clusterEnd(int[] codepoints, int start, int end) {
        if (start >= end) {
            return end;
        }
        int first = codepoints[start];
        int index = start + 1;
        if (first == '\n') {
            return index;
        }
        if (isRegionalIndicator(first)) {
            return (index < end && isRegionalIndicator(codepoints[index])) ? index + 1 : index;
        }
        while (index < end) {
            int next = codepoints[index];
            if (isExtender(next)) {
                index++;
            } else if (next == ZERO_WIDTH_JOINER && index + 1 < end && isPictographic(codepoints[index + 1])) {
                index += 2;
            } else {
                break;
            }
        }
        return index;
    }

    /**
     * Whether a cluster should be drawn with emoji presentation (by a color emoji font, when one is available).
     */
    static boolean isEmojiCluster(int[] codepoints, int start, int end) {
        int first = codepoints[start];
        if (isRegionalIndicator(first)) {
            return true;
        }
        boolean hasEmojiExtender = false;
        for (int i = start + 1; i < end; i++) {
            int codepoint = codepoints[i];
            if (codepoint == TEXT_PRESENTATION_SELECTOR) {
                return false;
            }
            if (codepoint == EMOJI_PRESENTATION_SELECTOR || codepoint == COMBINING_KEYCAP || codepoint == ZERO_WIDTH_JOINER || isEmojiModifier(codepoint) || isTag(codepoint)) {
                hasEmojiExtender = true;
            }
        }
        return hasEmojiExtender || prefersEmojiPresentation(first);
    }

    static boolean isRegionalIndicator(int codepoint) {
        return codepoint >= 0x1F1E6 && codepoint <= 0x1F1FF;
    }

    static boolean isEmojiModifier(int codepoint) {
        return codepoint >= 0x1F3FB && codepoint <= 0x1F3FF;
    }

    static boolean isTag(int codepoint) {
        return codepoint >= 0xE0020 && codepoint <= 0xE007F;
    }

    static boolean isVariationSelector(int codepoint) {
        return (codepoint >= 0xFE00 && codepoint <= 0xFE0F) || (codepoint >= 0xE0100 && codepoint <= 0xE01EF);
    }

    /**
     * Codepoints that never start a cluster of their own and attach to the preceding character instead.
     */
    static boolean isExtender(int codepoint) {
        if (isVariationSelector(codepoint) || isEmojiModifier(codepoint) || isTag(codepoint) || codepoint == COMBINING_KEYCAP) {
            return true;
        }
        int type = Character.getType(codepoint);
        return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    /**
     * Approximation of the Unicode {@code Extended_Pictographic} property, used to detect zero-width-joiner emoji sequences.
     */
    static boolean isPictographic(int codepoint) {
        if (codepoint >= 0x1F000 && codepoint <= 0x1FAFF) {
            return !isRegionalIndicator(codepoint) && !isEmojiModifier(codepoint);
        }
        if (codepoint >= 0x2600 && codepoint <= 0x27BF) {
            return true;
        }
        if (codepoint >= 0x2300 && codepoint <= 0x23FF) {
            return true;
        }
        if (codepoint >= 0x2B05 && codepoint <= 0x2B55) {
            return true;
        }
        return switch (codepoint) {
            case 0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25C0, 0x2934, 0x2935, 0x3030, 0x303D, 0x3297, 0x3299 -> true;
            default -> (codepoint >= 0x2194 && codepoint <= 0x21AA) || (codepoint >= 0x25FB && codepoint <= 0x25FE);
        };
    }

    /**
     * Approximation of the Unicode {@code Emoji_Presentation} property: characters that show as emoji even without U+FE0F.
     * Text-default symbols like U+00A9 or U+2764 only get emoji presentation together with U+FE0F.
     */
    static boolean prefersEmojiPresentation(int codepoint) {
        if (codepoint >= 0x1F000) {
            return isPictographic(codepoint);
        }
        return switch (codepoint) {
            case 0x231A, 0x231B, 0x23E9, 0x23EA, 0x23EB, 0x23EC, 0x23F0, 0x23F3, 0x25FD, 0x25FE, 0x2614, 0x2615, 0x267F, 0x2693, 0x26A1,
                 0x26AA, 0x26AB, 0x26BD, 0x26BE, 0x26C4, 0x26C5, 0x26CE, 0x26D4, 0x26EA, 0x26F2, 0x26F3, 0x26F5, 0x26FA, 0x26FD, 0x2705,
                 0x270A, 0x270B, 0x2728, 0x274C, 0x274E, 0x2753, 0x2754, 0x2755, 0x2757, 0x2795, 0x2796, 0x2797, 0x27B0, 0x27BF, 0x2B1B,
                 0x2B1C, 0x2B50, 0x2B55 -> true;
            default -> codepoint >= 0x2648 && codepoint <= 0x2653;
        };
    }

}
