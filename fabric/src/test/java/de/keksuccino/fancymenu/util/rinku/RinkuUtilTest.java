package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RinkuUtilTest {

    @Test
    void presenceCheckUsesRinkuModId() {
        AtomicReference<String> checkedModId = new AtomicReference<>();

        assertTrue(RinkuUtil.isRinkuPresent(modId -> {
            checkedModId.set(modId);
            return true;
        }));
        assertEquals("rinku", checkedModId.get());
    }

    @Test
    void presenceCheckReturnsLoaderResult() {
        assertFalse(RinkuUtil.isRinkuPresent(modId -> false));
    }

}
