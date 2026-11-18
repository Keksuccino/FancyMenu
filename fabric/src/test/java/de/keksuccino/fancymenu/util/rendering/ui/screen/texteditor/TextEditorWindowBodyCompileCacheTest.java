package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// The compile cache is shared with the placeholder parser, which runs compileSingleLineString for every parsed text
@ResourceLock("PlaceholderParser global state")
class TextEditorWindowBodyCompileCacheTest {

    private static final String NEWLINE = TextEditorWindowBody.NEWLINE_CODE;
    private static final String SPACE = TextEditorWindowBody.SPACE_CODE;

    @Test
    void removesLineBreakAndSpaceCodes() {
        assertEquals("{\"placeholder\":\"echo\"}", TextEditorWindowBody.compileSingleLineString("{" + NEWLINE + SPACE + SPACE + "\"placeholder\":\"echo\"" + NEWLINE + "}"));
        assertEquals("", TextEditorWindowBody.compileSingleLineString(NEWLINE + SPACE));
        assertEquals("", TextEditorWindowBody.compileSingleLineString(""));
        assertNull(TextEditorWindowBody.compileSingleLineString(null));
    }

    @Test
    void removesSpaceCodesThatOnlyFormAfterRemovingLineBreakCodes() {
        // "%!s" + NEWLINE + "!%" turns into SPACE once NEWLINE is gone
        assertEquals("ab", TextEditorWindowBody.compileSingleLineString("a%!s" + NEWLINE + "!%b"));
    }

    @Test
    void stringWithoutCodesCompilesToItself() {
        String text = "Singleplayer";
        assertSame(text, TextEditorWindowBody.compileSingleLineString(text));
        assertSame(text, TextEditorWindowBody.compileSingleLineString(text));
    }

    @Test
    void repeatedCallsReturnTheSameCompiledInstance() {
        String text = "first" + NEWLINE + "second";
        String compiled = TextEditorWindowBody.compileSingleLineString(text);

        assertEquals("firstsecond", compiled);
        assertSame(compiled, TextEditorWindowBody.compileSingleLineString(text));
        assertSame(compiled, TextEditorWindowBody.compileSingleLineString(new String(text.toCharArray())));
    }

    @Test
    void clearingTheCacheCompilesAgain() {
        String text = "first" + NEWLINE + "second";
        String compiled = TextEditorWindowBody.compileSingleLineString(text);

        TextEditorWindowBody.clearCompiledSingleLineCache();

        String recompiled = TextEditorWindowBody.compileSingleLineString(text);
        assertEquals(compiled, recompiled);
        assertNotSame(compiled, recompiled);
        assertSame(recompiled, TextEditorWindowBody.compileSingleLineString(text));
    }

    @Test
    void cacheKeepsAtMostTheMaximumNumberOfEntries() {
        String first = "first" + NEWLINE + "text";
        String compiledFirst = TextEditorWindowBody.compileSingleLineString(first);
        String latest = null;
        String compiledLatest = null;
        for (int i = 0; i < TextEditorWindowBody.COMPILED_SINGLE_LINE_STRINGS_MAX_ENTRIES; i++) {
            latest = "text " + i + NEWLINE;
            compiledLatest = TextEditorWindowBody.compileSingleLineString(latest);
        }

        // The oldest entry made room for the last one, the newer ones are still cached
        String recompiledFirst = TextEditorWindowBody.compileSingleLineString(first);
        assertEquals(compiledFirst, recompiledFirst);
        assertNotSame(compiledFirst, recompiledFirst);
        assertSame(compiledLatest, TextEditorWindowBody.compileSingleLineString(latest));
    }

    @Test
    void stringsHeavierThanTheWeightLimitStillCompileButAreNotCached() {
        String huge = "x".repeat((int) TextEditorWindowBody.COMPILED_SINGLE_LINE_STRINGS_MAX_WEIGHT) + NEWLINE;
        String compiled = TextEditorWindowBody.compileSingleLineString(huge);
        String compiledAgain = TextEditorWindowBody.compileSingleLineString(huge);

        assertEquals(huge.length() - NEWLINE.length(), compiled.length());
        assertEquals(compiled, compiledAgain);
        assertNotSame(compiled, compiledAgain);
    }

    @Test
    void concurrentCallsForTheSameStringAllGetTheSameInstance() throws Exception {
        int callers = 8;
        for (int round = 0; round < 50; round++) {
            String text = "round " + round + NEWLINE + "of concurrent compiling" + SPACE;
            List<String> results = ConcurrentTestCalls.invoke(callers, () -> TextEditorWindowBody.compileSingleLineString(text));
            String expected = results.get(0);
            assertEquals("round " + round + "of concurrent compiling", expected);
            for (String result : results) assertSame(expected, result);
            assertSame(expected, TextEditorWindowBody.compileSingleLineString(text));
        }
    }

    @Test
    void concurrentCallsForManyStringsStayCorrect() throws Exception {
        int callers = 8;
        int stringsPerCaller = 5_000;
        AtomicInteger nextCaller = new AtomicInteger();
        List<Integer> wrongResults = ConcurrentTestCalls.invoke(callers, () -> {
            int caller = nextCaller.getAndIncrement();
            int wrong = 0;
            for (int i = 0; i < stringsPerCaller; i++) {
                // Half of the strings are shared by all callers, the other half is distinct per caller
                String id = ((i % 2) == 0) ? ("shared " + (i % 500)) : ("caller " + caller + " string " + i);
                String compiled = TextEditorWindowBody.compileSingleLineString(id + NEWLINE + "end" + SPACE);
                if (!compiled.equals(id + "end")) wrong++;
            }
            return wrong;
        });

        List<Integer> expected = new ArrayList<>();
        for (int caller = 0; caller < callers; caller++) expected.add(0);
        assertEquals(expected, wrongResults);
        String shared = "shared 0" + NEWLINE + "end" + SPACE;
        assertSame(TextEditorWindowBody.compileSingleLineString(shared), TextEditorWindowBody.compileSingleLineString(shared));
    }

}
