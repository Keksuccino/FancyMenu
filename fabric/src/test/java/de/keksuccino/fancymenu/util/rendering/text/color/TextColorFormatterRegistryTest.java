package de.keksuccino.fancymenu.util.rendering.text.color;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.awt.Color;
import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ResourceLock("TextColorFormatterRegistry global state")
class TextColorFormatterRegistryTest {

    private static final char CODE = 'ø';
    private static final char SHARED_CODE = 'å';
    private static final char UNUSED_CODE = 'þ';
    private static final TextColorFormatter FORMATTER = new TextColorFormatter(CODE, DrawableColor.of(new Color(1, 2, 3)));
    private static final TextColorFormatter FIRST_SHARED_CODE_FORMATTER = new TextColorFormatter(SHARED_CODE, DrawableColor.of(new Color(4, 5, 6)));
    private static final TextColorFormatter SECOND_SHARED_CODE_FORMATTER = new TextColorFormatter(SHARED_CODE, DrawableColor.of(new Color(7, 8, 9)));

    @BeforeAll
    static void registerFormatters() {
        TextColorFormatterRegistry.register("text_color_formatter_registry_test", FORMATTER);
        TextColorFormatterRegistry.register("text_color_formatter_registry_test_shared_code_1", FIRST_SHARED_CODE_FORMATTER);
        TextColorFormatterRegistry.register("text_color_formatter_registry_test_shared_code_2", SECOND_SHARED_CODE_FORMATTER);
    }

    @Test
    void formatterGetsFoundByItsCodeAndItsIdentifier() {
        assertSame(FORMATTER, TextColorFormatterRegistry.getByCode(CODE));
        assertSame(FORMATTER, TextColorFormatterRegistry.getFormatter("text_color_formatter_registry_test"));
        assertTrue(TextColorFormatterRegistry.getFormatters().contains(FORMATTER));
        assertNull(TextColorFormatterRegistry.getByCode(UNUSED_CODE));
    }

    @Test
    void codeSharedByTwoFormattersFindsTheOneTheFormatterListHoldsFirst() {
        TextColorFormatter firstInList = null;
        for (TextColorFormatter formatter : TextColorFormatterRegistry.getFormatters()) {
            if (formatter.getCode() == SHARED_CODE) {
                firstInList = formatter;
                break;
            }
        }

        assertSame(firstInList, TextColorFormatterRegistry.getByCode(SHARED_CODE));
    }

    @Test
    void formatterRegisteredLaterGetsFoundAsWell() {
        char code = 'æ';
        assertNull(TextColorFormatterRegistry.getByCode(code));

        TextColorFormatter formatter = new TextColorFormatter(code, DrawableColor.of(new Color(10, 11, 12)));
        TextColorFormatterRegistry.register("text_color_formatter_registry_test_later", formatter);

        assertSame(formatter, TextColorFormatterRegistry.getByCode(code));
        assertSame(FORMATTER, TextColorFormatterRegistry.getByCode(CODE));
    }

    @Test
    void lookupByCodeAllocatesNothing() {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        int lookups = 100000;
        // The first calls load all classes, which keeps their one-time allocations out of the measured calls
        TextColorFormatterRegistry.getByCode(CODE);
        TextColorFormatterRegistry.getByCode(UNUSED_CODE);

        int found = 0;
        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < lookups; i++) {
            if (TextColorFormatterRegistry.getByCode(CODE) != null) found++;
            if (TextColorFormatterRegistry.getByCode(UNUSED_CODE) != null) found++;
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertEquals(lookups, found);
        // A copy of the formatter list alone takes more than 40 bytes per lookup, which adds up to 8 MB here
        assertTrue(allocated < 256L * 1024L, () -> "Allocated " + allocated + " bytes for " + (lookups * 2) + " lookups");
    }

}
