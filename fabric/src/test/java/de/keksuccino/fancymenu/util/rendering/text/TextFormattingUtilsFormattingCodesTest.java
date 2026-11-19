package de.keksuccino.fancymenu.util.rendering.text;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TextFormattingUtilsFormattingCodesTest {

    private static final String ALL_CODES = "0123456789abcdeflmnokr";
    // Codes, their uppercase forms, characters right next to the code ranges, prefixes, escapes and characters outside of Latin-1
    private static final char[] INPUT_CHARACTERS = "&§&§0159acfklmnor0AFKLR/:`ghjpqsxz %\\\"{}\nä中😀".toCharArray();
    private static final String[][] PREFIX_PAIRS = {
            {"&", "§"}, {"§", "&"}, {"&", "&"}, {"§", "§"}, {"&", ""}, {"", "§"}, {"", ""}, {"&&", "§"},
            {"§", "&&"}, {"&§", "§&"}, {"%", "§§§"}, {"\ud83d", "\ude00"}, {"中", "&"},
            // Prefixes with code characters get the replacement passes one by one
            {"a", "§"}, {"&", "a"}, {"aa", "x"}, {"0&", "&0"}, {"", "a"}, {"r", "k"}, {"&r", "&"}, {"ab", "ba"}, {"x0", "0x"}
    };

    @Test
    void convertsEveryLowercaseCode() {
        StringBuilder in = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (char code : ALL_CODES.toCharArray()) {
            in.append('&').append(code).append(' ');
            expected.append('§').append(code).append(' ');
        }
        assertEquals(expected.toString(), TextFormattingUtils.replaceFormattingCodes(in.toString(), "&", "§"));
        assertEquals(in.toString(), TextFormattingUtils.replaceFormattingCodes(expected.toString(), "§", "&"));
    }

    @Test
    void keepsEverythingThatIsNoLowercaseCode() {
        assertEquals("&A&F&K&L&R&g&j&p&q&s&x&z& &/&:&`", TextFormattingUtils.replaceFormattingCodes("&A&F&K&L&R&g&j&p&q&s&x&z& &/&:&`", "&", "§"));
        assertEquals("abc&", TextFormattingUtils.replaceFormattingCodes("abc&", "&", "§"));
        assertEquals("&", TextFormattingUtils.replaceFormattingCodes("&", "&", "§"));
        assertEquals("", TextFormattingUtils.replaceFormattingCodes("", "&", "§"));
    }

    @Test
    void handlesPrefixesNextToEachOther() {
        assertEquals("&§a", TextFormattingUtils.replaceFormattingCodes("&&a", "&", "§"));
        assertEquals("§a§b&", TextFormattingUtils.replaceFormattingCodes("&a&b&", "&", "§"));
        assertEquals("&§a", TextFormattingUtils.replaceFormattingCodes("&&&a", "&&", "§"));
        assertEquals("a&b", TextFormattingUtils.replaceFormattingCodes("&a&&b", "&", ""));
        assertEquals("§a§1x", TextFormattingUtils.replaceFormattingCodes("a1x", "", "§"));
    }

    @Test
    void matchesThePassByPassConversionForFixedCases() {
        String[] inputs = {"", "&", "§", "&&a", "&a&", "a&", "&A", "&&", "&a&b&c", "aaa0", "aa0a", "x0x0a", "0&0&a", "rrk", "&r&rr", "abab0", "😀a", "中0中a"};
        for (String[] prefixes : PREFIX_PAIRS) {
            for (String input : inputs) assertMatchesPassByPass(input, prefixes[0], prefixes[1]);
        }
    }

    @Test
    void matchesThePassByPassConversionForGeneratedTexts() {
        Random random = new Random(20261119L);
        List<String[]> prefixPairs = new ArrayList<>(List.of(PREFIX_PAIRS));
        char[] prefixCharacters = "&§a0rKx".toCharArray();
        for (int i = 0; i < 300; i++) prefixPairs.add(new String[] {randomText(random, prefixCharacters, 3), randomText(random, prefixCharacters, 3)});
        for (String[] prefixes : prefixPairs) {
            for (int i = 0; i < 400; i++) {
                assertMatchesPassByPass(randomText(random, INPUT_CHARACTERS, 40), prefixes[0], prefixes[1]);
                assertMatchesPassByPass(randomTextOfPrefixesAndCodes(random, prefixes[0], 12), prefixes[0], prefixes[1]);
            }
        }
    }

    @Test
    void returnsTheTextItselfWhenNothingGetsConverted() {
        String[] inputs = {"Singleplayer", "&A&x &", "", "§a", "&&&"};
        for (String input : inputs) assertSame(input, TextFormattingUtils.replaceFormattingCodes(input, "&", "§"));
        String plain = "no codes here";
        assertSame(plain, TextFormattingUtils.replaceFormattingCodes(plain, "aa", "x"));
    }

    @Test
    void convertsLongTextsWithManyCodes() {
        StringBuilder in = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 4_000; i++) {
            char code = ALL_CODES.charAt(i % ALL_CODES.length());
            in.append("&&").append(code).append("&Z text ");
            expected.append("&§").append(code).append("&Z text ");
        }
        assertEquals(expected.toString(), TextFormattingUtils.replaceFormattingCodes(in.toString(), "&", "§"));
    }

    @Test
    void textsWithoutCodesGetCheckedWithoutAllocating() {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        String label = "Singleplayer";
        String longText = "word & word &Z ".repeat(1_000);
        // The first calls load all classes, which keeps their one-time allocations out of the measured calls
        TextFormattingUtils.replaceFormattingCodes(label, "&", "§");
        TextFormattingUtils.replaceFormattingCodes(longText, "&", "§");

        int calls = 10_000;
        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        int checked = 0;
        for (int i = 0; i < calls; i++) {
            checked += TextFormattingUtils.replaceFormattingCodes(label, "&", "§").length();
            checked += TextFormattingUtils.replaceFormattingCodes(longText, "&", "§").length();
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertEquals(calls * (label.length() + longText.length()), checked);
        // Building the 22 search strings for one call alone takes more than 1 KB
        assertTrue(allocated < 64L * 1024L, () -> "Allocated " + allocated + " bytes for " + (2 * calls) + " calls");
    }

    @Test
    void formattingCodeStartsAtEveryPrefixWithACodeBehindIt() {
        for (char code : ALL_CODES.toCharArray()) {
            assertTrue(TextFormattingUtils.isFormattingCodeAt("&" + code, 0), () -> "&" + code);
            assertTrue(TextFormattingUtils.isFormattingCodeAt("§" + code, 0), () -> "§" + code);
            assertTrue(TextFormattingUtils.isFormattingCodeAt("x§" + code + "x", 1), () -> "§" + code);
            if (Character.isLetter(code)) {
                char uppercase = Character.toUpperCase(code);
                // Minecraft reads the codes behind § case-insensitively, but only lowercase codes behind & get converted
                assertTrue(TextFormattingUtils.isFormattingCodeAt("§" + uppercase, 0), () -> "§" + uppercase);
                assertFalse(TextFormattingUtils.isFormattingCodeAt("&" + uppercase, 0), () -> "&" + uppercase);
            }
        }
    }

    @Test
    void noFormattingCodeStartsAtOtherCharactersOrAtTheEnd() {
        String[] noCodes = {"&g", "§g", "§G", "&&", "§§", "& a", "§ a", "&/", "&:", "&`", "§`", "aa", "a0", "%a"};
        for (String text : noCodes) assertFalse(TextFormattingUtils.isFormattingCodeAt(text, 0), text);
        assertFalse(TextFormattingUtils.isFormattingCodeAt("&", 0));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("a&", 1));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("a§", 1));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("&a", 1));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("&a", 2));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("", 0));
        assertTrue(TextFormattingUtils.isFormattingCodeAt("&&a", 1));
        assertFalse(TextFormattingUtils.isFormattingCodeAt("&&a", 0));
    }

    @Test
    void ampersandCodesAreExactlyTheCodesTheConversionConverts() {
        Random random = new Random(20261120L);
        for (int i = 0; i < 20_000; i++) {
            String text = randomText(random, INPUT_CHARACTERS, 30);
            StringBuilder expected = new StringBuilder(text);
            for (int index = 0; index < text.length(); index++) {
                if ((text.charAt(index) == '&') && TextFormattingUtils.isFormattingCodeAt(text, index)) expected.setCharAt(index, '§');
            }
            assertEquals(expected.toString(), TextFormattingUtils.replaceFormattingCodes(text, "&", "§"), text);
        }
    }

    private static void assertMatchesPassByPass(String input, String oldPrefix, String newPrefix) {
        assertEquals(replaceFormattingCodesPassByPass(input, oldPrefix, newPrefix), TextFormattingUtils.replaceFormattingCodes(input, oldPrefix, newPrefix), () -> "Different result for \"" + input + "\" with prefixes \"" + oldPrefix + "\" -> \"" + newPrefix + "\"");
    }

    private static String randomText(Random random, char[] characters, int maximumLength) {
        int length = random.nextInt(maximumLength + 1);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append(characters[random.nextInt(characters.length)]);
        return text.toString();
    }

    private static String randomTextOfPrefixesAndCodes(Random random, String prefix, int maximumParts) {
        int parts = random.nextInt(maximumParts + 1);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts; i++) {
            int kind = random.nextInt(4);
            if (kind == 0) {
                text.append(prefix);
            } else if (kind == 1) {
                text.append(ALL_CODES.charAt(random.nextInt(ALL_CODES.length())));
            } else if (kind == 2) {
                text.append(prefix).append(ALL_CODES.charAt(random.nextInt(ALL_CODES.length())));
            } else {
                text.append(INPUT_CHARACTERS[random.nextInt(INPUT_CHARACTERS.length)]);
            }
        }
        return text.toString();
    }

    /**
     * The conversion as it was first written: one {@link StringUtils#replace(String, String, String)} pass per code, in this order.
     * It defines the results that {@link TextFormattingUtils#replaceFormattingCodes(String, String, String)} has to keep.
     */
    private static String replaceFormattingCodesPassByPass(String in, String oldPrefix, String newPrefix) {
        in = StringUtils.replace(in, oldPrefix + "0", newPrefix + "0");
        in = StringUtils.replace(in, oldPrefix + "1", newPrefix + "1");
        in = StringUtils.replace(in, oldPrefix + "2", newPrefix + "2");
        in = StringUtils.replace(in, oldPrefix + "3", newPrefix + "3");
        in = StringUtils.replace(in, oldPrefix + "4", newPrefix + "4");
        in = StringUtils.replace(in, oldPrefix + "5", newPrefix + "5");
        in = StringUtils.replace(in, oldPrefix + "6", newPrefix + "6");
        in = StringUtils.replace(in, oldPrefix + "7", newPrefix + "7");
        in = StringUtils.replace(in, oldPrefix + "8", newPrefix + "8");
        in = StringUtils.replace(in, oldPrefix + "9", newPrefix + "9");
        in = StringUtils.replace(in, oldPrefix + "a", newPrefix + "a");
        in = StringUtils.replace(in, oldPrefix + "b", newPrefix + "b");
        in = StringUtils.replace(in, oldPrefix + "c", newPrefix + "c");
        in = StringUtils.replace(in, oldPrefix + "d", newPrefix + "d");
        in = StringUtils.replace(in, oldPrefix + "e", newPrefix + "e");
        in = StringUtils.replace(in, oldPrefix + "f", newPrefix + "f");
        in = StringUtils.replace(in, oldPrefix + "l", newPrefix + "l");
        in = StringUtils.replace(in, oldPrefix + "m", newPrefix + "m");
        in = StringUtils.replace(in, oldPrefix + "n", newPrefix + "n");
        in = StringUtils.replace(in, oldPrefix + "o", newPrefix + "o");
        in = StringUtils.replace(in, oldPrefix + "k", newPrefix + "k");
        in = StringUtils.replace(in, oldPrefix + "r", newPrefix + "r");
        return in;
    }

}
